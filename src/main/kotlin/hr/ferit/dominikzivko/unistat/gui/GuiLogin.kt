package hr.ferit.dominikzivko.unistat.gui

import domyutil.jfx.*
import hr.ferit.dominikzivko.unistat.App
import hr.ferit.dominikzivko.unistat.gui.component.Prompt
import hr.ferit.dominikzivko.unistat.gui.component.PromptCompanion
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.StackPane
import javafx.stage.Stage

data class LoginPromptResult(
    override val cancelled: Boolean,
    val goOffline: Boolean,
    val username: String,
    val password: String,
    val remember: Boolean
) : Prompt.Result

class GuiLogin : Prompt {
    @FXML
    private lateinit var header: StackPane

    @FXML
    private lateinit var usernameField: TextField

    @FXML
    private lateinit var passwordField: PasswordField

    @FXML
    private lateinit var remember: CheckBox

    @FXML
    private lateinit var errorMessageLabel: Label

    private var cancelled = false
    private var goOffline = false

    override val stage get() = usernameField.scene.window as Stage

    var errorMessage: String?
        get() = errorMessageLabel.text?.ifBlank { null }
        set(value) {
            errorMessageLabel.text = value
        }

    @FXML
    private fun initialize() {
        header.apply {
            val dragHandler = ManualStageDrag()
            onMousePressed = dragHandler
            onMouseDragged = dragHandler
            onMouseReleased = dragHandler
        }
    }

    @FXML
    override fun accept() {
        stage.close()
    }

    @FXML
    override fun cancel() {
        cancelled = true
        stage.close()
    }

    @FXML
    fun goOffline() {
        goOffline = true
        stage.close()
    }

    override fun reset() {
        clearInputs()
        cancelled = false
        goOffline = false
        remember.isSelected = false
        errorMessage = null
    }

    fun clearInputs() {
        usernameField.clear()
        passwordField.clear()
    }

    override fun makeResult() = LoginPromptResult(
        cancelled,
        goOffline,
        usernameField.text,
        passwordField.text,
        remember.isSelected
    )

    companion object Companion : PromptCompanion {
        override val fxmlPath = "Login.fxml"
        override val title = "${App.APPNAME} - ${strings["title_login"]}"
    }
}