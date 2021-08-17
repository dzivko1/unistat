package domyutil.jfx

import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
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
        showAlert(
            Alert.AlertType.ERROR,
            message,
            title ?: strings["error"],
            blocking,
            TextArea(throwable.stackTraceToString())
        )
    }

    fun confirmation(
        message: String,
        logger: Logger? = null,
        title: String? = null,
        blocking: Boolean = true
    ): Boolean {
        logger?.info("Showing confirmation message: $message")
        val alert = showAlert(Alert.AlertType.CONFIRMATION, message, title, blocking)
        return alert.result == ButtonType.OK
    }

    private fun showAlert(
        alertType: Alert.AlertType,
        message: String,
        title: String? = null,
        blocking: Boolean = true,
        expandableContent: Node? = null
    ) = runFxAndWait<Alert> {
        Alert(alertType).apply {
            this.title = title
            contentText = message
            dialogPane.expandableContent = expandableContent
            if (blocking) showAndWait()
            else show()
        }
    }


}