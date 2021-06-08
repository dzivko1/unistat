package domyutil.jfx

import javafx.application.Platform
import javafx.beans.property.Property
import javafx.beans.value.ObservableValue
import javafx.event.Event
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import kotlin.reflect.KProperty

val strings by lazy { ResourceBundle.getBundle("Strings") }
operator fun ResourceBundle.get(key: String): String = getString(key)

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>): T = value
operator fun <T> Property<T>.setValue(thisRef: Any, property: KProperty<*>, value: T?) = setValue(value)

/**
 * Run the specified block on the JavaFX Application Thread at some unspecified time in the future, or
 * immediately if called from that thread. If called from a thread that is not the JavaFX Application Thread, this
 * method will post the specified block to the event queue and then return immediately to the caller.
 * @param block code that needs to be executed on the JavaFX Application Thread
 * @see Platform.runLater
 */
fun runFx(block: () -> Unit) =
    if (Platform.isFxApplicationThread()) block()
    else Platform.runLater(block)

/**
 * Run the specified block on the JavaFX Application Thread at some unspecified time in the future and wait until
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

fun <T> runFxAndWait(block: () -> T) : T {
    val task = FutureTask(block)
    if (Platform.isFxApplicationThread()) task.run()
    else Platform.runLater(task)
    return task.get()
}

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