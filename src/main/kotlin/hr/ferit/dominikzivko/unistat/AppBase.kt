package hr.ferit.dominikzivko.unistat

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.gui.UIManager
import javafx.beans.property.SimpleBooleanProperty
import javafx.stage.Stage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.ext.inject
import java.util.concurrent.TimeUnit

class AppBase(
    val uiManager: UIManager,
    val repository: Repository
) {
    private val log by lazy { LogManager.getLogger(javaClass) }

    val offlineModeProperty = SimpleBooleanProperty(this, "offlineMode", false)
    var offlineMode: Boolean by offlineModeProperty

    fun start(primaryStage: Stage) {
        uiManager.primaryStage = primaryStage
        uiManager.start()
        repository.start()
        refreshUserData()
    }

    fun stop() {
        uiManager.stop()
        repository.stop()
        taskExecutor.shutdownNow()
        taskExecutor.awaitTermination(10, TimeUnit.SECONDS)
    }

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
                    else -> throw BackgroundTaskException(it, shouldExit = !uiManager.isBaseGuiShowing)
                }
            }

        }
        uiManager.showBaseGui()
    }

    fun exportFilteredBills() {
        exportBills(repository.filteredBills)
    }

    private fun exportBills(bills: List<Bill>) {
        val location = uiManager.showSaveDialog(
            title = strings["title_exportAs"],
            extensionFilters = App.billFileExtensionFilters
        ) ?: return

        log.info("Exporting ${bills.size} bills to $location.")
        val json = Json.encodeToString(bills)
        location.writeText(json)
    }

    fun logout() = runBackground {
        uiManager.monitorProgress(strings["loggingOut"]) {
            repository.forget()
        }
        setupOnlineMode()
    }

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