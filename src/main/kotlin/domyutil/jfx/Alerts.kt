package domyutil.jfx

import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import org.apache.logging.log4j.Logger

object Alerts {
    fun catching(
        message: String,
        throwable: Throwable,
        logger: Logger? = null,
        title: String? = null,
        blocking: Boolean = true
    ) {
        logger?.error("Error message: $message", throwable)

        runFxAndWait {
            Alert(Alert.AlertType.ERROR).apply {
                if (title != null) setTitle(title)
                contentText = message
                dialogPane.expandableContent = TextArea(throwable.stackTraceToString())
                if (blocking) showAndWait()
                else show()
            }
        }
    }
}