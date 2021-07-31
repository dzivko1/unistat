package hr.ferit.dominikzivko.unistat.gui.component

import domyutil.jfx.*
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.VBox
import javafx.stage.Stage

/**
 * A GUI component which can show progress in the form of a progress bar and a textual message. The monitor can include
 * a cancel button which can be configured to perform any action when clicked.
 *
 * This progress monitor has the ability to export its state via the [ProgressMonitor.State] class, enabling the state
 * to be imported back at a later time.
 */
class ProgressMonitor(
    initialMessage: String = "",
    initialProgress: Double = -1.0,
    initialOnCancel: (() -> Unit)? = null
) : VBox() {

    /** The percentage of progress shown by the progress bar. */
    val progressProperty = SimpleDoubleProperty(this, "progress", initialProgress)
    var progress: Number by progressProperty

    /** The message describing the current relevant information about the action being performed. */
    val messageProperty = SimpleStringProperty(this, "message", initialMessage)
    var message: String by messageProperty

    /** The action to be performed when the cancel button is clicked. Can be null, in which case the button is not shown. */
    val onCancelProperty = SimpleObjectProperty<(() -> Unit)?>(this, "onCancel", initialOnCancel)
    var onCancel: (() -> Unit)? by onCancelProperty

    val stage: Stage get() = scene.window as Stage

    init {
        prefWidth = 350.0
        minWidth = 350.0
        padding = Insets(15.0)
        spacing = 10.0
        isFillWidth = true
        alignment = Pos.CENTER

        val messageLabel = Label().apply {
            maxWidth = Double.MAX_VALUE
            textProperty().bind(messageProperty)
        }

        val progressBar = ProgressBar().apply {
            maxWidth = Double.MAX_VALUE
            progressProperty().bind(progressProperty)
        }

        val cancelButton = Button(strings["btn_cancel"]).apply {
            isCancelButton = true
            visibleProperty().bind(onCancelProperty.isNotNull)
            setOnAction {
                onCancel?.invoke()
            }
        }

        children.addAll(messageLabel, progressBar, cancelButton)
    }

    /**
     * Calls the specified function block with this value as its receiver on the FX thread.
     * @param wait if true, waits for the block to be executed before returning
     */
    fun applyFx(wait: Boolean = false, block: ProgressMonitor.() -> Unit) {
        if (wait) runFxAndWait { block() }
        else runFx { block() }
    }

    fun show() = runFx { stage.show() }

    fun hide() = runFx { stage.hide() }

    /**
     * Export the monitor state to a new [ProgressMonitor.State] object.
     */
    fun exportState() = State(message, progress as Double, onCancel)

    /**
     * Import the monitor state from a specified [ProgressMonitor.State] object.
     */
    fun importState(state: State, wait: Boolean = false) = applyFx(wait) {
        message = state.message
        progress = state.progress
        onCancel = state.onCancel
    }


    /**
     * A data class storing information about the state of a [ProgressMonitor].
     */
    data class State(
        val message: String,
        val progress: Double,
        val onCancel: (() -> Unit)?
    )
}