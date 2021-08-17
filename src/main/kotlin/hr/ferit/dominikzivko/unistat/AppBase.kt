package hr.ferit.dominikzivko.unistat

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.gui.UIManager
import javafx.beans.property.SimpleBooleanProperty
import javafx.stage.Stage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.ext.inject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The base class of the application. It contains most of the app context and enables running top-level actions such as
 * refreshing the user's data.
 *
 * The first layer of [AppComponent]s is stored here. Their start and stop methods are called from methods in this class
 * which have the same name (even though this class does not officially implement the AppComponent interface).
 */
class AppBase(
    val uiManager: UIManager,
    val repository: Repository
) {
    private val log by lazy { LogManager.getLogger(javaClass) }

    /** Whether the data is sourced locally or remotely from the webserver. */
    val offlineModeProperty = SimpleBooleanProperty(this, "offlineMode")
    var offlineMode: Boolean by offlineModeProperty

    /**
     * Initializes the application state at startup.
     * @param primaryStage the primary stage provided by JavaFx
     */
    fun start(primaryStage: Stage, offlineMode: Boolean) {
        uiManager.primaryStage = primaryStage
        this.offlineMode = offlineMode
        uiManager.start()
        repository.start()
        refreshUserData()
    }

    /** Prepares the application for shutdown. */
    fun stop() {
        uiManager.stop()
        repository.stop()
        taskExecutor.shutdownNow()
        taskExecutor.awaitTermination(10, TimeUnit.SECONDS)
    }

    /**
     * A top-level action that refreshes the user's data. The action is posted to a background thread and run
     * asynchronously.
     *
     * The way data is refreshed depends on the current offline/online state of the application. The result of this
     * action can change that state and cause this action to be run again recursively.
     */
    fun refreshUserData() = runBackground {
        uiManager.monitorProgress { monitor ->
            val theThread = Thread.currentThread() as BackgroundThread
            monitor.applyFx { onCancel = { theThread.cancelled = true } }

            runCatching {
                repository.refresh(monitor)
            }.onFailure {
                when (it) {
                    is OpenExportedBillsException -> {
                        setupOfflineMode()
                        return@runBackground
                    }
                    is InputCancelledException -> {
                        log.info("Login cancelled by user.")
                        App.exit()
                        return@runBackground
                    }
                    is CancellationException -> {
                        if (offlineMode) {
                            setupOnlineMode()
                            return@runBackground
                        } else log.info("Data refresh cancelled by user.")
                    }
                    is IOException -> {
                        Alerts.catching(strings["msg_communicationError"], it)
                        val shouldRetry = Alerts.confirmation(strings["msg_retryRefresh"])
                        if (shouldRetry) setupOnlineMode()
                        else App.exit()
                        return@runBackground
                    }
                    else -> throw BackgroundTaskException(it, shouldExit = !uiManager.isBaseGuiShowing)
                }
            }

        }
        uiManager.showBaseGui()
    }

    /**
     * A top-level action that imports a collection of bills from a file selected by the user to the pool of existing
     * bills which are already loaded.
     */
    fun importBills() {
        val location = uiManager.showOpenDialog(
            title = strings["title_import"],
            extensionFilters = App.billFileExtensionFilters
        ) ?: return

        uiManager.monitorProgress(strings["importingBills"]) {
            runBackground {
                log.info("Importing bills from $location")
                val json = location.readText()
                val decoded = Json.decodeFromString<List<Bill>>(json)
                repository.importBills(decoded)
            }
        }
    }

    /**
     * A top-level action that exports the collection of bills currently showing in the application to a file selected
     * by the user.
     */
    fun exportFilteredBills() {
        exportBills(repository.filteredBills)
    }

    private fun exportBills(bills: List<Bill>) {
        val location = uiManager.showSaveDialog(
            title = strings["title_exportAs"],
            extensionFilters = App.billFileExtensionFilters
        ) ?: return

        uiManager.monitorProgress(strings["exportingBills"]) {
            runBackground {
                log.info("Exporting ${bills.size} bills to $location.")
                val json = Json.encodeToString(bills)
                location.writeText(json)
            }
        }
    }

    /**
     * A top-level action that logs the user out of the application and the webserver (if applicable), and reinitiates
     * the app in online mode, causing a new data refresh and subsequently a login prompt.
     */
    fun logout() = runBackground {
        uiManager.monitorProgress(strings["loggingOut"]) {
            repository.forget()
        }
        setupOnlineMode()
    }

    /**
     * Ensures that the app is in online mode and starts the data refresh action
     */
    private fun setupOnlineMode() {
        if (offlineMode) {
            log.info("Setting up online mode.")
            unloadKoinModules(localDatasourceModule)
            loadKoinModules(remoteDatasourceModule)
            repository::dataSource.inject()
            offlineMode = false
        }
        refreshUserData()
    }

    /**
     * Ensures that the app is in offline mode and starts the data refresh action.
     */
    private fun setupOfflineMode() {
        if (!offlineMode) {
            log.info("Setting up offline mode.")
            unloadKoinModules(remoteDatasourceModule)
            loadKoinModules(localDatasourceModule)
            repository::dataSource.inject()
            offlineMode = true
        }
        refreshUserData()
    }
}