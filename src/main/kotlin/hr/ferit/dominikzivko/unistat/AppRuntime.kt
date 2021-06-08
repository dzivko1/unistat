package hr.ferit.dominikzivko.unistat

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.ui.GuiLogin
import hr.ferit.dominikzivko.unistat.ui.LoginPromptResult
import hr.ferit.dominikzivko.unistat.ui.UIManager
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.web.AuthWebConnection
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AppRuntime(
    val uiManager: UIManager
) {
    private val log by lazy { LogManager.getLogger(javaClass) }
    val repository: Repository get() = GlobalContext.get().get()
    val authConn: AuthWebConnection get() = GlobalContext.get().get()

    private var _offlineMode: Boolean? = null
    var offlineMode: Boolean
        get() = _offlineMode!!
        private set(value) {
            _offlineMode = value
        }

    fun start(primaryStage: Stage) {
        uiManager.primaryStage = primaryStage
        try {
            authorizeUser()
            repository.reload()
            refreshUserData()
            uiManager.showBaseGui()
        } catch (e: InputCanceledException) {
            log.info("Login canceled.")
            App.exit()
        }
    }

    private fun refreshUserData() {
        uiManager.monitorProgress { monitor ->
            repository.refresh(monitor)
        }
    }

    @Throws(InputCanceledException::class)
    private fun authorizeUser() {
        if (Pref.autoLogin) {
            setupOnlineMode()
            verifyAutoLogin().takeIf { it.not() }?.let {
                disableAutoLogin()
                askForLogin()
            }
        } else {
            askForLogin()
        }
    }

    @Throws(InputCanceledException::class)
    private fun askForLogin() {
        val loginPrompt: GuiLogin = uiManager.loadPrompt(GuiLogin::class)
        lateinit var input: LoginPromptResult
        var loggedIn = false

        while (!loggedIn) {
            loginPrompt.clearInputs()
            input = runFxAndWait<LoginPromptResult> { loginPrompt.acquireInput() as LoginPromptResult }

            if (input.cancelled) {
                throw InputCanceledException()
            }

            if (input.useSampleData) {
                setupOfflineMode()
                loggedIn = true
            } else {
                setupOnlineMode()
                verifyLogin(input.username, input.password).also { result ->
                    loggedIn = result.first
                    loginPrompt.errorMessage = result.second
                }
            }
        }

        if (input.remember) {
            enableAutoLogin(input.username, input.password)
        }
    }

    private fun setupOnlineMode() {
        if (_offlineMode == true) {
            unloadKoinModules(localDatasourceModule)
        }
        if (_offlineMode != false) {
            log.info("Setting up online mode.")
            _offlineMode = false
            loadKoinModules(remoteDatasourceModule)
        }
    }

    private fun setupOfflineMode() {
        if (_offlineMode == false) {
            unloadKoinModules(remoteDatasourceModule)
        }
        if (_offlineMode != true) {
            log.info("Setting up offline mode.")
            _offlineMode = true
            loadKoinModules(localDatasourceModule)
        }
    }

    private fun enableAutoLogin(username: String, password: String) {
        log.info("Enabling auto-login.")
        Pref.savedUsername = obfuscate(username)
        Pref.savedPassword = obfuscate(password)
        Pref.autoLogin = true
    }

    private fun disableAutoLogin() {
        log.info("Disabling auto-login.")
        Pref.savedUsername = ""
        Pref.savedPassword = ""
        Pref.autoLogin = false
    }

    private fun verifyAutoLogin(): Boolean {
        log.info("Performing auto-login...")
        return uiManager.monitorProgress(strings["logging_in"]) {
             try {
                authConn.login(deobfuscate(Pref.savedUsername), deobfuscate(Pref.savedPassword))
                log.info("Auto-login successful.")
                true
            } catch (e: LoginFailedException) {
                log.warn("Auto-login failed. Message: ${e.userFriendlyMessage}")
                false
            }
        }
    }

    private fun verifyLogin(username: String, password: String): Pair<Boolean, String?> {
        log.info("Verifying user login with the webserver...")
        return uiManager.monitorProgress(strings["logging_in"]) {
            try {
                authConn.login(username, password)
                log.info("User is successfully authenticated.")
                Pair(true, null)
            } catch (e: LoginFailedException) {
                log.info("User verification failed.")
                Pair(false, e.userFriendlyMessage ?: strings["msg_error_occurred"])
            }
        }
    }
}