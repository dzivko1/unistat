package hr.ferit.dominikzivko.unistat

/**
 * A logical section of the application which are initialized and uninitialized by calling the [start] and [stop] methods.
 */
interface AppComponent {

    /** Initialize this component at app startup. */
    fun start()

    /** Uninitialize this component at app shutdown. */
    fun stop()

}