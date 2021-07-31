package hr.ferit.dominikzivko.unistat

import domyutil.jfx.*
import org.apache.logging.log4j.LogManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executor service for background tasks.
 */
val taskExecutor: ExecutorService = Executors.newCachedThreadPool(BackgroundThreadFactory)

/**
 * Creates [BackgroundThread]s.
 */
private object BackgroundThreadFactory : ThreadFactory {
    const val THREAD_NAME_PREFIX = "worker-thread-"
    private val threadCount = AtomicInteger()
    private val handler = Thread.UncaughtExceptionHandler { t, e ->
        Alerts.catching(strings["msg_errorOccurred"], e, LogManager.getLogger(t.javaClass))
        if (e is BackgroundTaskException && e.shouldExit) App.exit()
    }

    override fun newThread(r: Runnable) =
        BackgroundThread(r, THREAD_NAME_PREFIX + threadCount.incrementAndGet()).apply {
            uncaughtExceptionHandler = handler
        }
}

/**
 * A thread used for executing background tasks. The tasks have the option of being cancellable.
 * @see BackgroundThread.cancelled
 * @see checkCancelled
 */
class BackgroundThread(target: Runnable, name: String) : Thread(target, name) {
    /**
     * Whether the task executed by this thread is marked for cancellation. This property can be used directly to cancel the task.
     * The cancellable code that is being run by this thread should (when appropriate) check for cancellation by
     * calling [checkCancelled].
     */
    var cancelled = false
}

/**
 * Submits the specified function block to be executed on a background thread.
 */
fun runBackground(shouldNest: Boolean = false, block: () -> Unit) {
    if (shouldNest || Thread.currentThread() !is BackgroundThread) {
        taskExecutor.execute(block)
    } else block()
}

/**
 * Throws a [CancellationException] if the calling thread is an instance of [BackgroundThread] and it has the
 * [BackgroundThread.cancelled] flag set to `true`.
 */
fun checkCancelled() {
    Thread.currentThread().let {
        if (it is BackgroundThread && it.cancelled) {
            it.cancelled = false
            throw CancellationException()
        }
    }
}