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