package hr.ferit.dominikzivko.unistat.ui.component

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

class ProgressMonitor(
    initialMessage: String = "",
    initialProgress: Double = -1.0,
    initialOnCancel: (() -> Unit)? = null
) : VBox() {

    val progressProperty = SimpleDoubleProperty(this, "progress", initialProgress)
    var progress: Number by progressProperty

    val messageProperty = SimpleStringProperty(this, "message", initialMessage)
    var message: String by messageProperty

    val onCancelProperty = SimpleObjectProperty<(() -> Unit)?>(this, "onCancel", initialOnCancel)
    var onCancel by onCancelProperty

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

    fun applyFx(wait: Boolean = false, block: ProgressMonitor.() -> Unit) {
        if (wait) runFxAndWait { block() }
        else runFx { block() }
    }

    fun show() = runFx { stage.show() }

    fun hide() = runFx { stage.hide() }

    fun exportState() = State(message, progress as Double, onCancel)

    fun importState(state: State, wait: Boolean = false) = applyFx(wait) {
        message = state.message
        progress = state.progress
        onCancel = state.onCancel
    }


    data class State(
        val message: String,
        val progress: Double,
        val onCancel: (() -> Unit)?
    )
}