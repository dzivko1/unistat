package hr.ferit.dominikzivko.unistat.web

import com.gargoylesoftware.htmlunit.html.DomNode
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput
import com.gargoylesoftware.htmlunit.html.HtmlTextInput
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.LoginFailedException
import hr.ferit.dominikzivko.unistat.UnexpectedResponseException
import hr.ferit.dominikzivko.unistat.data.Pref
import hr.ferit.dominikzivko.unistat.data.UserLogon
import hr.ferit.dominikzivko.unistat.urlString
import javafx.beans.property.ReadOnlyObjectWrapper
import org.apache.logging.log4j.LogManager

class AuthWebConnection(val webConnection: WebConnection) {
    private val log by lazy { LogManager.getLogger(javaClass) }

    private val _currentUserProperty = ReadOnlyObjectWrapper<UserLogon?>(this, "currentUser")
    val currentUserProperty get() = _currentUserProperty.readOnlyProperty
    var currentUser
        get() = _currentUserProperty.value
        private set(value) {
            _currentUserProperty.value = value
        }


    @Throws(LoginFailedException::class)
    fun fetchAuthorized(url: String): HtmlPage {
        require(!url.startsWith(Pref.url_loginBase)) { "A privileged connection cannot be made to the login page." }

        val response = webConnection.get(url)
        val responseUrl = response.urlString
        return if (!responseUrl.startsWith(Pref.url_loginBase)) response
        else authConnect(url, response)
    }

    @Throws(LoginFailedException::class)
    private fun authConnect(url: String, loginPage: HtmlPage): HtmlPage {
        log.info("Performing login as requested by the webserver...")

        if (currentUser == null)
            throw LoginFailedException("Attempted privileged connection without known login details.")

        doLogin(currentUser!!.username, currentUser!!.password, loginPage)

        val response = webConnection.get(url)
        val responseUrl = response.urlString
        if (!responseUrl.startsWith(Pref.url_loginBase)) return response
        else throw UnexpectedResponseException("Unexpected state after attempting a privileged connect to the webserver.")
    }

    @Throws(LoginFailedException::class)
    fun login(username: String, password: String, autoLogout: Boolean = true) {
        log.info("Logging on the webserver...")
        val response = webConnection.get(Pref.url_student)
        val responseUrl = response.urlString
        when {
            responseUrl.startsWith(Pref.url_loginBase) -> doLogin(username, password, response)
            responseUrl.startsWith(Pref.url_student) -> {
                if (autoLogout) {
                    log.info("Another user is already logged in. Will log out and reattempt login.")
                    logout()
                    login(username, password, false)
                    return
                } else {
                    log.info("Another user is already logged in. Login will not proceed.")
                    throw LoginFailedException("Another user is already logged in and automatic logout was disabled.")
                }
            }
            else -> {
                val msg = "Received unexpected server response: $responseUrl."
                log.error(msg)
                throw UnexpectedResponseException(msg)
            }
        }
    }

    @Throws(LoginFailedException::class)
    private fun doLogin(username: String, password: String, loginPage: HtmlPage) {
        require(loginPage.urlString.startsWith(Pref.url_loginBase)) {
            "The `loginPage` argument does not appear to be the expected webserver's login page. URL: ${loginPage.urlString}"
        }
        log.info("Submitting login info...")

        val response = with(loginPage.getFormByName("f")) {
            getInputByName<HtmlTextInput>("username").type(username)
            getInputByName<HtmlPasswordInput>("password").type(password)
            getButtonByName("Submit").click<HtmlPage>()
        }

        if (response.urlString.startsWith(Pref.url_loginBase)) {
            val errorMessage =
                response.querySelector<DomNode>(".error")?.asNormalizedText().takeUnless { it.isNullOrBlank() }
                    ?: strings["msg_error_occurred"]
            log.info("Login failed: \"$errorMessage\"")
            throw LoginFailedException("The webserver returned an error message: $errorMessage", errorMessage)
        }

        currentUser = UserLogon(username, password)
        log.info("Login successful.")
    }

    fun logout() {
        log.info("Logging off the webserver...")
        webConnection.get(Pref.url_logout)
        currentUser = null
        log.info("Logoff finished.")
    }
}