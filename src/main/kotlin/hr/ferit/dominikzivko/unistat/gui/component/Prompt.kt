package hr.ferit.dominikzivko.unistat.gui.component

import javafx.fxml.FXML
import javafx.stage.Stage

/**
 * A GUI construct which enables the user to input data in various forms. Prompts are designed to be made in FXML, and
 * implementations of this interface should be the controllers of those FXML views. Each implemented prompt controller
 * needs to have a companion object which implements [PromptCompanion] in order to have its title and path to their FXML
 * file clearly packaged along with it.
 *
 * A prompt provides a [Prompt.Result] object representing the user's response to the prompt. A prompt can be accepted or
 * cancelled, each of the outcomes being reflected in the Result object.
 *
 * The most basic usage of prompt includes calling [acquireInput] which will show the prompt to the user and await their
 * input, returning the result at the end.
 */
interface Prompt {

    /**
     * A representation of the user's input on a [Prompt]. This should be implemented by data classes with properties
     * representing the prompt's possible inputs.
     */
    interface Result {
        val cancelled: Boolean
    }

    /** The stage containing this prompt's view. */
    val stage: Stage

    /** Sets the prompt in an accepted state and closes the prompt stage. */
    fun accept()

    /** Sets the prompt in a cancelled state and closes the prompt stage. */
    fun cancel()

    /** Resets the input state of the prompt. */
    fun reset()

    /** Returns a new result object representing the input state of the prompt at the time of calling. */
    fun makeResult(): Result

    /** Simply shows the stage containing the prompt view. */
    fun show() = stage.show()

    /** Simply shows the stage containing the prompt view and waits for it to close before returning. */
    fun showAndWait() = stage.showAndWait()

    /** Shows the prompt to the user and waits for it to be closed, returning the prompt result afterwards. */
    fun acquireInput(): Result {
        showAndWait()
        return makeResult()
    }
}

/**
 * An interface that companion objects of [Prompt] implementations should implement in order to reliably define static
 * information about the prompt, such as the path to its FXML definition.
 */
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

