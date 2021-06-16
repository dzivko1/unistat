package hr.ferit.dominikzivko.unistat

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.data.*
import hr.ferit.dominikzivko.unistat.ui.UIManager
import javafx.stage.Stage
import org.apache.logging.log4j.LogManager
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.ext.inject
import java.util.concurrent.*
import kotlin.properties.Delegates

class AppBase(
    val uiManager: UIManager,
    val repository: Repository
) {
    private val log by lazy { LogManager.getLogger(javaClass) }

    var sampleMode by Delegates.notNull<Boolean>()
        private set

    fun start(primaryStage: Stage, sampleMode: Boolean) {
        this.sampleMode = sampleMode
        uiManager.start()
        uiManager.primaryStage = primaryStage
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
                    is SwitchToSampleException -> {
                        setupSampleMode()
                        return@runBackground
                    }
                    is InputCancelledException -> {
                        log.info("Login cancelled by user.")
                        App.exit()
                        return@runBackground
                    }
                    is CancellationException -> log.info("Data refresh cancelled by user.")
                    else -> throw BackgroundTaskException(it, shouldExit = !uiManager.isBaseGuiShowing)
                }
            }

        }
        uiManager.showBaseGui()
    }

    fun logout() = runBackground {
        uiManager.monitorProgress(strings["logging_out"]) {
            repository.forget()
        }
        setupOnlineMode()
    }

    private fun setupOnlineMode() {
        if (sampleMode) {
            log.info("Setting up online mode.")
            unloadKoinModules(sampleDatasourceModule)
            loadKoinModules(remoteDatasourceModule)
            repository::dataSource.inject()
            sampleMode = false
        }
        refreshUserData()
    }

    private fun setupSampleMode() {
        if (!sampleMode) {
            log.info("Setting up sample mode.")
            unloadKoinModules(remoteDatasourceModule)
            loadKoinModules(sampleDatasourceModule)
            repository::dataSource.inject()
            sampleMode = true
        }
        refreshUserData()
    }
}