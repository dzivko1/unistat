package hr.ferit.dominikzivko.unistat.web

import com.gargoylesoftware.htmlunit.html.*
import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.LoginDetails
import hr.ferit.dominikzivko.unistat.data.Pref
import hr.ferit.dominikzivko.unistat.data.UserLogon
import hr.ferit.dominikzivko.unistat.ui.UIManager
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext

class AuthWebGateway(val web: WebGateway) : AppComponent {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val uiManager: UIManager get() = GlobalContext.get().get()

    var currentUser: UserLogon? = null
        private set

    private val isUserVerified: Boolean
        get() {
            if (currentUser == null)
                verifyUser()
            return currentUser != null
        }

    override fun start() {
        web.start()
    }

    override fun stop() {
        web.stop()
    }

    @Throws(InputCancelledException::class, SwitchToSampleException::class, NotLoggedInException::class)
    fun fetchAuthorized(
        url: String,
        progressMonitor: ProgressMonitor? = null,
        canPromptLogin: Boolean = false
    ): HtmlPage {
        require(!url.startsWith(Pref.url_loginBase)) { "A privileged connection cannot be made to the login page." }
        return web.get(url).let { page ->
            checkCancelled()
            if (!page.isLoginPage && isUserVerified) page
            else authConnect(url, page, progressMonitor, canPromptLogin)
        }
    }

    @Throws(InputCancelledException::class, SwitchToSampleException::class, NotLoggedInException::class)
    private fun authConnect(
        url: String,
        loginPage: HtmlPage,
        progressMonitor: ProgressMonitor?,
        canPromptLogin: Boolean,
        loginErrorMessage: String? = null
    ): HtmlPage {
        requireLoginPage(loginPage)
        log.info("Webserver requested authentication.")

        val loginDetails = when {
            Pref.autoLogin -> LoginDetails(Pref.savedUsername, Pref.savedPassword)
            canPromptLogin -> {
                progressMonitor?.applyFx { stage.hide() }
                askForLogin(loginErrorMessage).also { progressMonitor?.applyFx { stage.show() } }
            }
            else -> throw NotLoggedInException()
        }

        val progressState = progressMonitor?.exportState()
        progressMonitor?.applyFx {
            message = strings["loggingIn"]
            progress = -1
            onCancel = null
        }

        try {
            login(loginDetails, loginPage)
        } catch (e: LoginFailedException) {
            val errorMessage = e.userFriendlyMessage ?: strings["msg_errorOccurred"]
            val freshLoginPage = web.get(Pref.url_student).takeIf { it.isLoginPage }
                ?: throw UnexpectedResponseException("Did not receive the login page during a login attempt.")
            progressMonitor?.importState(progressState!!, wait = true)
            return authConnect(url, freshLoginPage, progressMonitor, canPromptLogin, errorMessage)
        }

        progressMonitor?.importState(progressState!!)
        checkCancelled()

        return web.get(url).let { page ->
            if (!page.isLoginPage) page
            else throw UnexpectedResponseException("Received the login page after a privileged connection.")
        }
    }

    @Throws(InputCancelledException::class, SwitchToSampleException::class)
    private fun askForLogin(errorMessage: String? = null): LoginDetails {
        uiManager.promptLogin(errorMessage).run {
            if (cancelled) throw InputCancelledException()
            if (useSampleData) throw SwitchToSampleException()

            return LoginDetails(username, password)
                .also { if (remember) enableAutoLogin(it) }
        }
    }

    @Throws(LoginFailedException::class)
    private fun login(loginDetails: LoginDetails, loginPage: HtmlPage) {
        requireLoginPage(loginPage)
        log.info("Logging on the webserver...")

        val response = with(loginPage.getFormByName("f")) {
            getInputByName<HtmlTextInput>("username").type(loginDetails.username)
            getInputByName<HtmlPasswordInput>("password").type(loginDetails.password)
            getButtonByName("Submit").click<HtmlPage>()
        }

        if (response.isLoginPage) {
            val errorMessage =
                response.querySelector<DomNode>(".error")?.asNormalizedText().takeUnless { it.isNullOrBlank() }
            log.info("Login failed: \"$errorMessage\"")

            if (Pref.autoLogin) disableAutoLogin()

            throw LoginFailedException(
                "The webserver returned an error message: $errorMessage",
                errorMessage ?: strings["msg_errorOccurred"]
            )
        }

        if (Pref.autoLogin)
            storeUserCredentials(loginDetails.username, response)

        currentUser = UserLogon(loginDetails.username)
        log.info("Login successful.")
    }

    fun logout() {
        log.info("Logging off the webserver...")
        web.get(Pref.url_logout)
        currentUser = null
        disableAutoLogin()
        log.info("Logoff finished.")
    }

    private fun storeUserCredentials(username: String, page: HtmlPage) {
        log.info("Storing user credentials.")
        val studentPage = ensureStudentPage(page)
        val userOib = extractUserOib(studentPage)
        Pref.userCredentials = obfuscate("$username|$userOib")
    }

    private fun verifyUser() {
        log.info("Verifying user credentials.")
        val studentPage = ensureStudentPage(web.lastPage)
        val actualOib = extractUserOib(studentPage)
        val (username, expectedOib) = deobfuscate(Pref.userCredentials).split('|')

        if (actualOib != expectedOib) {
            logout()
            throw IllegalStateException("The user logged on the webserver does not have expected credentials.")
        }

        currentUser = UserLogon(username)
    }

    private fun ensureStudentPage(page: HtmlPage?) = page?.takeIf { it.isStudentPage }
        ?: web.get(Pref.url_student).takeIf { it.isStudentPage }
        ?: throw UnexpectedResponseException("Could not obtain student page.")

    private fun extractUserOib(studentPage: HtmlPage): String {
        val userInfoDiv =
            studentPage.querySelector<HtmlDivision>("section:nth-of-type(1) .row:nth-of-type(2) .col:nth-of-type(1)")
        return userInfoDiv.asNormalizedText().substringAfter("OIB: ").takeWhile { it.isDigit() }
    }

    private fun enableAutoLogin(loginDetails: LoginDetails) {
        log.info("Enabling auto-login.")
        Pref.savedUsername = obfuscate(loginDetails.username)
        Pref.savedPassword = obfuscate(loginDetails.password)
        Pref.autoLogin = true
    }

    private fun disableAutoLogin() {
        if (!Pref.autoLogin) return
        log.info("Disabling auto-login.")
        Pref.userCredentials = ""
        Pref.savedUsername = ""
        Pref.savedPassword = ""
        Pref.autoLogin = false
    }


    private fun requireLoginPage(page: HtmlPage) = require(page.isLoginPage) {
        "The page does not appear to be the expected webserver's login page. URL: ${page.urlString}"
    }

    private val HtmlPage.isLoginPage get() = urlString.startsWith(Pref.url_loginBase)
    private val HtmlPage.isStudentPage get() = urlString.startsWith(Pref.url_student)
}