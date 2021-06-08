package hr.ferit.dominikzivko.unistat.ui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.App
import hr.ferit.dominikzivko.unistat.ui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.ui.component.Prompt
import hr.ferit.dominikzivko.unistat.ui.component.PromptCompanion
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.apache.logging.log4j.LogManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance

val SERVER_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy. H:mm")

val FLOAT_FORMAT = DecimalFormat("0.00").apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply { decimalSeparator = ',' }
}

class UIManager {
    private val log by lazy { LogManager.getLogger(javaClass) }

    lateinit var primaryStage: Stage

    private lateinit var baseScene: Scene

    fun showBaseGui() = runFxAndWait {
        if (!this::baseScene.isInitialized)
            initBaseGui()

        log.info("Showing main window.")
        primaryStage.show()
    }

    private fun initBaseGui() {
        log.info("Loading main window.")
        baseScene = Scene(FXMLLoader(javaClass.getResource("/gui/Base.fxml")).load()).apply {
            stylesheets += App.mainStylesheet
        }
        primaryStage.apply {
            scene = baseScene
            title = strings["unistat"]
        }
    }

    fun <T : Prompt> loadPrompt(promptClass: KClass<T>): T = runFxAndWait<T> {
        val promptCompanion = (promptClass.companionObjectInstance as PromptCompanion)
        val loader = FXMLLoader(javaClass.getResource(promptCompanion.fxmlPath), ResourceBundle.getBundle("Strings"))
        Stage(StageStyle.UNDECORATED).apply {
            title = promptCompanion.title
            scene = Scene(loader.load()).apply {
                stylesheets += App.mainStylesheet
            }
        }
        return@runFxAndWait loader.getController()
    }

    fun <T> monitorProgress(initialMessage: String = "", initialProgress: Double = -1.0, block: (ProgressMonitor) -> T) =
        monitorProgress(ProgressMonitor(initialMessage, initialProgress), block)

    fun <T> monitorProgress(progressMonitor: ProgressMonitor, block: (ProgressMonitor) -> T): T {
        showProgressMonitor(progressMonitor)
        return block(progressMonitor).also { runFx { progressMonitor.stage.close() } }
    }

    fun showProgressMonitor(monitor: ProgressMonitor, title: String = strings["progress"]) = runFx {
        Stage(StageStyle.UTILITY).apply {
            this.title = title
            scene = Scene(monitor)
            show()
        }
    }
}