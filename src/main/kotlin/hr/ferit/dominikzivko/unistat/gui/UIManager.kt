/*
Some info:
    Animation on XYChart types has a bug where x axis labels are incorrectly drawn after being dynamically changed.
    That is why it is disabled (in FXML).
    Issue: JDK-8198830
*/

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

/** A date-time formatter which matches the date-time format used by the webserver. */
val SERVER_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy. H:mm")

/** A date-time formatter which formats a full date and time combination with a vertical-line separator. */
val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy. | H:mm")

/** A date-time formatter which formats a full date in a default manner. */
val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy.")

/** A date-time formatter which formats a full date in a short format. */
val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yy.")

/** A date-time formatter which formats a time instant with hours and minutes in a default manner. */
val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

/** A date-time formatter which formats a month-year combination in a default manner. */
val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy.")


/** A decimal format of pattern "0.00" with a comma (,) decimal separator. */
val FLOAT_FORMAT = DecimalFormat("0.00").apply {
    decimalFormatSymbols = DecimalFormatSymbols().apply { decimalSeparator = ',' }
}

/** A function which converts a floating point number to a UI-appropriate string. */
val floatToString = { float: Number -> float.toString("%.2f") }

/** A UI-oriented string representing a short form of the used currency. */
val shortCurrencyStr = "kn"


/**
 * An [AppComponent] handling the UI aspect of the application.
 */
class UIManager : AppComponent {
    private val log by lazy { LogManager.getLogger(javaClass) }

    /** A reference holder for the JavaFX framework-provided primary stage object. */
    lateinit var primaryStage: Stage

    /** A reference holder for the base scene. Used for determining the initialization state of the base GUI. */
    private lateinit var baseScene: Scene

    /** A lazily initialized [FileChooser] used for showing open and save dialogs. */
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

    /**
     * Shows the base GUI on the FX thread and waits for completion. The GUI is initialized when this is called for
     * the first time.
     */
    fun showBaseGui() = runFxAndWait {
        if (!this::baseScene.isInitialized)
            initBaseGui()

        primaryStage.show()
    }

    /** Hides the base GUI on the FX thread and waits for completion. */
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

    /**
     * Hides the base GUI if showing and shows a login prompt with an optional error message, then waits for user input.
     * This runs on the FX thread and returns after completion.
     * @param errorMessage an optional string to be shown as an error message
     * @return a [GuiLogin.Result] containing user input
     * @see GuiLogin
     */
    fun promptLogin(errorMessage: String? = null) = runFxAndWait<GuiLogin.Result> {
        log.info("Prompting user login.")
        hideBaseGui()
        loadPrompt(GuiLogin::class).run {
            this.errorMessage = errorMessage
            acquireInput() as GuiLogin.Result
        }
    }

    /**
     * Loads a new prompt based on the specified subclass of [Prompt] which has a companion object implementing the
     * [PromptCompanion] interface.
     * This runs on the FX thread and returns after completion.
     * @param promptClass a subclass of [Prompt]
     * @return a controller of the loaded prompt
     */
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

    /**
     * Shows an open dialog with the specified parameters and returns its result.
     * This runs on the FX thread and returns after completion.
     */
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

    /**
     * Shows a save dialog with the specified parameters and returns its result.
     * This runs on the FX thread and returns after completion.
     */
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

    /**
     * Shows a [ProgressMonitor] for the specified [block]. The shown monitor is passed as an argument to the block.
     * @return the result of the executed block
     */
    inline fun <T> monitorProgress(
        initialMessage: String = "",
        initialProgress: Double = -1.0,
        block: (ProgressMonitor) -> T
    ) = monitorProgress(ProgressMonitor(initialMessage, initialProgress), block)

    /**
     * Shows the specified [ProgressMonitor] monitoring the specified [block]. The shown monitor is passed as an
     * argument to the block.
     * @return the result of the executed block
     */
    inline fun <T> monitorProgress(
        progressMonitor: ProgressMonitor,
        block: (ProgressMonitor) -> T
    ): T {
        showProgressMonitor(progressMonitor)
        return block(progressMonitor).also {
            runFx { progressMonitor.hide() }
        }
    }

    /**
     * Shows the specified [ProgressMonitor].
     * This action is posted to the FX thread and returns immediately.
     */
    fun showProgressMonitor(monitor: ProgressMonitor, title: String = strings["progress"]) = runFx {
        Stage(StageStyle.UTILITY).apply {
            this.title = title
            scene = Scene(monitor)
            show()
        }
    }
}