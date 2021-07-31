package hr.ferit.dominikzivko.unistat.gui

import domyutil.*
import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.App
import hr.ferit.dominikzivko.unistat.AppComponent
import hr.ferit.dominikzivko.unistat.gui.component.ProgressMonitor
import hr.ferit.dominikzivko.unistat.gui.component.Prompt
import hr.ferit.dominikzivko.unistat.gui.component.PromptCompanion
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.apache.logging.log4j.LogManager
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/*
Some info:
    Animation on XYChart types has a bug where x axis labels are incorrectly drawn after being dynamically changed.
    That is why it is disabled (in FXML).
    Issue: JDK-8198830
*/

val SERVER_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy. H:mm")
val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy. | H:mm")
val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy.")
val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yy.")
val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy.")

val FLOAT_FORMAT = DecimalFormat("0.00").apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply { decimalSeparator = ',' }
}

val floatToString = { float: Number -> float.toString("%.2f") }

val shortCurrencyStr = "kn"

class UIManager : AppComponent {
    private val log by lazy { LogManager.getLogger(javaClass) }

    lateinit var primaryStage: Stage

    private lateinit var baseScene: Scene
    private val fileChooser by lazy { FileChooser() }

    val isBaseGuiShowing get() = primaryStage.isShowing

    override fun start() {
        Locale.setDefault(Locale.forLanguageTag("hr-HR"))
        primaryStage.icons += listOf(
            Image(javaClass.getResourceAsStream("images/UniStat-logo-16.png")),
            Image(javaClass.getResourceAsStream("images/UniStat-logo-32.png")),
            Image(javaClass.getResourceAsStream("images/UniStat-logo-64.png"))
        )
    }

    override fun stop() {}

    fun showBaseGui() = runFxAndWait {
        if (!this::baseScene.isInitialized)
            initBaseGui()

        primaryStage.show()
    }

    fun hideBaseGui() = runFxAndWait {
        primaryStage.hide()
    }

    private fun initBaseGui() {
        log.info("Loading main window.")
        baseScene = Scene(FXMLLoader(javaClass.getResource("Base.fxml"), ResourceBundle.getBundle("Strings")).load()).apply {
            stylesheets += App.mainStylesheet
        }
        primaryStage.apply {
            scene = baseScene
            title = strings["unistat"]
            setOnCloseRequest { App.exit() }
        }
    }

    fun promptLogin(errorMessage: String? = null) = runFxAndWait<GuiLogin.Result> {
        log.info("Prompting user login.")
        hideBaseGui()
        loadPrompt(GuiLogin::class).run {
            this.errorMessage = errorMessage
            acquireInput() as GuiLogin.Result
        }
    }

    private fun <T : Prompt> loadPrompt(promptClass: KClass<T>): T = runFxAndWait<T> {
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

    fun showOpenDialog(
        title: String? = null,
        initialDirectory: File? = null,
        initialFileName: String? = null,
        extensionFilters: List<FileChooser.ExtensionFilter>? = null,
        selectedExtensionFilter: FileChooser.ExtensionFilter? = null
    ): File? = runFxAndWait<File?> {
        fileChooser.configure(title, initialDirectory, initialFileName, extensionFilters, selectedExtensionFilter)
        return@runFxAndWait fileChooser.showOpenDialog(primaryStage)
    }

    fun showSaveDialog(
        title: String? = null,
        initialDirectory: File? = null,
        initialFileName: String? = null,
        extensionFilters: List<FileChooser.ExtensionFilter>? = null,
        selectedExtensionFilter: FileChooser.ExtensionFilter? = null
    ): File? = runFxAndWait<File?> {
        fileChooser.configure(title, initialDirectory, initialFileName, extensionFilters, selectedExtensionFilter)
        return@runFxAndWait fileChooser.showSaveDialog(primaryStage)
    }

    private fun FileChooser.configure(
        title: String?,
        initialDirectory: File?,
        initialFileName: String?,
        extensionFilters: List<FileChooser.ExtensionFilter>?,
        selectedExtensionFilter: FileChooser.ExtensionFilter?
    ) {
        this.title = title
        this.initialDirectory = initialDirectory
        this.initialFileName = initialFileName
        this.extensionFilters.setAll(extensionFilters ?: emptyList())
        this.selectedExtensionFilter = selectedExtensionFilter
    }

    inline fun <T> monitorProgress(
        initialMessage: String = "",
        initialProgress: Double = -1.0,
        block: (ProgressMonitor) -> T
    ) = monitorProgress(ProgressMonitor(initialMessage, initialProgress), block)

    inline fun <T> monitorProgress(
        progressMonitor: ProgressMonitor,
        block: (ProgressMonitor) -> T
    ): T {
        showProgressMonitor(progressMonitor)
        return block(progressMonitor).also {
            runFx { progressMonitor.hide() }
        }
    }

    fun showProgressMonitor(monitor: ProgressMonitor, title: String = strings["progress"]) = runFx {
        Stage(StageStyle.UTILITY).apply {
            this.title = title
            scene = Scene(monitor)
            show()
        }
    }
}