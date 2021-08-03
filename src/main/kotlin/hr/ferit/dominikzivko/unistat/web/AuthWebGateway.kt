package hr.ferit.dominikzivko.unistat.web

import com.gargoylesoftware.htmlunit.html.*
import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.*
import hr.ferit.dominikzivko.unistat.data.LoginDetails
import hr.ferit.dominikzivko.unistat.data.Pref
import hr.ferit.dominikzivko.unistat.data.UserLogon
import hr.ferit.dominikzivko.unistat.gui.UIManager
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext

/**
 * An [AppComponent] handling authorized communication with the webserver. [WebGateway] is used to talk to the webserver.
 *
 * The user will be asked to log in to the webserver if at the time of requested access to a web resource the webserver
 * responds with a login page and the application is not permitted to, or does not possess the means of automatically
 * logging the user in.
 *
 * During the user's login input, they can choose to switch to offline mode (open exported bills), or cancel the input.
 * In those situations, [OpenExportedBillsException] or [InputCancelledException] will be thrown appropriately.
 *
 * Due to the webserver's unpredictability, there exists a possibility of an unexpected response, in case of which the
 * appropriate [UnexpectedResponseException] will be thrown.
 */
class AuthWebGateway(val web: WebGateway) : AppComponent {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val uiManager: UIManager get() = GlobalContext.get().get()

    /** The user currently considered logged on the webserver and, as such, the application. */
    var currentUser: UserLogon? = null
        private set

    /** Determines whether the user that is supposedly logged on the webserver is the user that is actually stored in the app. */
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

    /**
     * Fetches a web page denoted by the specified URL, fulfilling any authentication requirements imposed by the webserver.
     *
     * In the case of a required login, the app shall attempt an automated login if it possesses the means and is
     * permitted to do so, otherwise the user will be continually asked to login manually until a successful login
     * is made (assuming [canPromptLogin] is true). If at any point in the process occurs an event which prevents this
     * method from completing meaningfully, an appropriate exception will be thrown as described in this class' documentation.
     *
     * @return the resulting page of trying to connect with the specified URL
     *
     * @throws NotLoggedInException if [canPromptLogin] is false, and the user cannot be automatically logged in when
     * requested by the webserver
     * @throws InputCancelledException if the user cancels the login prompt when asked to log in
     * @throws OpenExportedBillsException if the user chooses to switch to offline mode
     */
    @Throws(InputCancelledException::class, OpenExportedBillsException::class, NotLoggedInException::class)
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

    /**
     * Attempts to login the user either automatically or with their input, and re-fetch the specified resource.
     * This process is recursively repeated until a successful login is made, or if stopped by an exception, as
     * described in this class' documentation.
     */
    @Throws(InputCancelledException::class, OpenExportedBillsException::class, NotLoggedInException::class)
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
            Pref.autoLogin -> LoginDetails(deobfuscate(Pref.savedUsername), deobfuscate(Pref.savedPassword))
            canPromptLogin -> {
                progressMonitor?.hide()
                askForLogin(loginErrorMessage).also { progressMonitor?.show() }
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

    /**
     * Prompts the user to log in to the webserver and returns the input [LoginDetails]. If cancelled or asked to open
     * exported bills, the appropriate exception is thrown.
     */
    @Throws(InputCancelledException::class, OpenExportedBillsException::class)
    private fun askForLogin(errorMessage: String? = null): LoginDetails {
        uiManager.promptLogin(errorMessage).run {
            if (cancelled) throw InputCancelledException()
            if (openExportedBills) throw OpenExportedBillsException()

            return LoginDetails(username, password)
                .also { if (remember) enableAutoLogin(it) }
        }
    }

    /**
     * Performs a login to the specified [loginPage] with the specified [loginDetails], throwing a [LoginFailedException]
     * with a user-friendly error message in case of failure.
     */
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

    /**
     * Logs the user out from the webserver, clears cookies and disables auto-login if enabled.
     */
    fun logout() {
        log.info("Logging off the webserver...")
        web.get(Pref.url_logout)
        web.clearCookies()
        currentUser = null
        disableAutoLogin()
        log.info("Logoff finished.")
    }

    /**
     * Stores the user's identifying information to be used for confirmation later.
     */
    private fun storeUserCredentials(username: String, page: HtmlPage) {
        log.info("Storing user credentials.")
        val studentPage = ensureStudentPage(page)
        val userOib = extractUserOib(studentPage)
        Pref.userCredentials = obfuscate("$username|$userOib")
    }

    /**
     * Verifies that the webserver's returned data belongs to the user that the application expects.
     */
    private fun verifyUser() {
        log.info("Verifying user credentials.")
        if (Pref.userCredentials.isEmpty()) return

        val studentPage = ensureStudentPage(web.lastPage)
        val actualOib = extractUserOib(studentPage)
        val (username, expectedOib) = deobfuscate(Pref.userCredentials).split('|')

        if (actualOib != expectedOib) {
            logout()
            throw IllegalStateException("The user logged on the webserver does not have expected credentials.")
        }

        currentUser = UserLogon(username)
    }

    /**
     * Returns the specified page if it is the login page, otherwise tries to fetch it.
     */
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