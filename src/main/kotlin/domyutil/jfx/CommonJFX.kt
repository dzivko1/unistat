package domyutil.jfx

import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import javafx.util.Callback
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import kotlin.reflect.KProperty

/*
Delegated property accessors for JavaFX properties.
 */
operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>): T = value
operator fun <T> WritableValue<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?) = setValue(value)


/**
 * A [ResourceBundle] for localization strings.
 */
val strings: ResourceBundle by lazy { ResourceBundle.getBundle("Strings") }
operator fun ResourceBundle.get(key: String): String = getString(key)

/**
 * Run the specified function block on the JavaFX Application Thread at some unspecified time in the future, or immediately if
 * called from that thread. If called from a thread other than the JavaFX Application Thread, this method will post
 * the specified block to the event queue and then return immediately to the caller.
 * @param block code that needs to be executed on the JavaFX Application Thread
 * @see Platform.runLater
 */
fun runFx(block: () -> Unit): Unit =
    if (Platform.isFxApplicationThread()) block()
    else Platform.runLater(block)

/**
 * Run the specified function block on the JavaFX Application Thread at some unspecified time in the future, or immediately if
 * called from that thread. The block is wrapped in a [FutureTask] before being executed. If called from a thread other
 * than the JavaFX Application Thread, this method will post the task to the event queue and then return immediately to
 * the caller.
 * @param block code that needs to be executed on the JavaFX Application Thread
 * @return a [FutureTask] representing the future result of the block
 */
fun <T> runFx(block: () -> T): Future<T> {
    val task = FutureTask(block)
    if (Platform.isFxApplicationThread()) task.run()
    else Platform.runLater(task)
    return task
}

/**
 * Run the specified function block on the JavaFX Application Thread at some unspecified time in the future and wait until
 * it is executed. If this is called from the JavaFX Application Thread, the runnable is run immediately instead of
 * being posted to the event queue.
 * @param block code that needs to be executed on the JavaFX Application Thread
 * @throws InterruptedException if interrupted while waiting for the block to complete execution
 * @see Platform.runLater
 */
@Throws(InterruptedException::class)
fun runFxAndWait(block: () -> Unit) {
    if (Platform.isFxApplicationThread()) {
        block()
        return
    }
    val doneLatch = CountDownLatch(1)
    Platform.runLater {
        try {
            block()
        } finally {
            doneLatch.countDown()
        }
    }
    doneLatch.await()
}

/**
 * Run the specified function block on the JavaFX Application Thread at some unspecified time in the future and wait until
 * it is executed. The block is wrapped in a [FutureTask] before being executed. If this is called from the JavaFX
 * Application Thread, the runnable is run immediately instead of being posted to the event queue.
 * @param block code that needs to be executed on the JavaFX Application Thread
 * @return a [FutureTask] representing the future result of the block
 * @throws InterruptedException if interrupted while waiting for the block to complete execution
 */
fun <T> runFxAndWait(block: () -> T): T = runFx(block).get()

/**
 * An event handler that prevents direct deselection of a selected toggle in a [ToggleGroup].
 * This event handler should be added as an event filter to toggles which should not be directly deselected.
 */
class DeselectionFilter(val toggleGroup: ToggleGroup) : EventHandler<Event> {
    override fun handle(event: Event) {
        if (
            event.source == toggleGroup.selectedToggle &&
            (event.eventType == MouseEvent.MOUSE_RELEASED || event.eventType == KeyEvent.KEY_RELEASED)
        ) {
            event.consume()
        }
    }
}

/**
 * An event handler that handles manual dragging of the stage by the component to which this handler is attached.
 * This event handler should be registered for `onMousePressed`, `onMouseDragged` and `onMouseReleased` events of the
 * component that should have the ability drag the stage.
 */
class ManualStageDrag : EventHandler<MouseEvent> {
    private var dragging = false
    private var dragX = 0.0
    private var dragY = 0.0

    override fun handle(event: MouseEvent) = with(event) {
        when (eventType) {
            MouseEvent.MOUSE_PRESSED -> {
                dragX = sceneX
                dragY = sceneY
            }
            MouseEvent.MOUSE_DRAGGED -> {
                val stage = (source as Node).scene.window as Stage
                if (!dragging) {
                    dragging = true
                    stage.opacity = 0.9
                }
                stage.x = screenX - dragX
                stage.y = screenY - dragY
            }
            MouseEvent.MOUSE_RELEASED -> {
                val stage = (source as Node).scene.window as Stage
                stage.opacity = 1.0
                dragging = false
            }
        }
    }
}

/**
 * A cell factory for [TableCell]s which can format an item object of a defined type to a string.
 */
abstract class StringCellFactory<S, T> : Callback<TableColumn<S, T>, TableCell<S, T>> {

    override fun call(param: TableColumn<S, T>?): TableCell<S, T> {
        return object : TableCell<S, T>() {
            override fun updateItem(item: T, empty: Boolean) {
                super.updateItem(item, empty)
                text = if (item == null || empty) null else format(item)
            }
        }
    }

    /**
     * Formats the specified item of type [T] to a string to be shown as text in a [TableCell].
     */
    abstract fun format(item: T): String
}

/**
 * Configures a [TableColumn] to have a [StringCellFactory] with the specified [format] function as its cell factory.
 */
fun <S, T> TableColumn<S, T>.setStringCellFactory(format: (item: T) -> String) {
    cellFactory = object : StringCellFactory<S, T>() {
        override fun format(item: T) = format(item)
    }
}