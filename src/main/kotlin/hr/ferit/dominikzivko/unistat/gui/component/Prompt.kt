package hr.ferit.dominikzivko.unistat.gui.component

import javafx.stage.Stage

interface Prompt {

    interface Result {
        val cancelled: Boolean
    }

    val stage: Stage

    fun accept()
    fun cancel()
    fun reset()
    fun makeResult(): Result

    fun show() = stage.show()
    fun showAndWait() = stage.showAndWait()
    fun acquireInput(): Result {
        showAndWait()
        return makeResult()
    }
}

interface PromptCompanion {
    val fxmlPath: String
    val title: String
}

abstract class PromptBase : Prompt {

    protected var cancelled = false

    @FXML
    override fun accept() {
        cancelled = false
        stage.close()
    }

    @FXML
    override fun cancel() {
        cancelled = true
        stage.close()
    }
}

