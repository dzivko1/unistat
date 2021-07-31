package domyutil.jfx

import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import org.apache.logging.log4j.Logger

/**
 * A utility for creating and showing GUI alert messages to the user of the application.
 */
object Alerts {

    /**
     * Shows an error message informing the user that a [Throwable] has been caught somewhere in the application.
     * The throwable's stack trace is shown as expandable content.
     */
    fun catching(
        message: String,
        throwable: Throwable,
        logger: Logger? = null,
        title: String? = null,
        blocking: Boolean = true
    ) {
        logger?.error("Showing exception message: $message", throwable)
        runFxAndWait {
            Alert(Alert.AlertType.ERROR).apply {
                setTitle(title ?: strings["error"])
                contentText = message
                dialogPane.expandableContent = TextArea(throwable.stackTraceToString())
                if (blocking) showAndWait()
                else show()
            }
        }
    }

}