package hr.ferit.dominikzivko.unistat.ui.component

import domyutil.jfx.*
import javafx.stage.Stage
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread

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

