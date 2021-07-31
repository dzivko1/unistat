package hr.ferit.dominikzivko.unistat

/**
 * A runtime exception representing an unexpected webserver response.
 */
open class UnexpectedResponseException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

/**
 * An exception indicating that the user is not properly logged in.
 */
open class NotLoggedInException : Exception()

/**
 * An exception indicating that the login to the webserver has failed.
 *
 * In addition to the regular exception message, this exception can hold a user friendly message to be shown to the user
 * in the UI.
 */
open class LoginFailedException : Exception {
    val userFriendlyMessage: String?

    constructor() : super() {
        this.userFriendlyMessage = null
    }

    constructor(message: String, userFriendlyMessage: String? = null) : super(message) {
        this.userFriendlyMessage = userFriendlyMessage
    }
}

/**
 * An exception indicating that an action has been cancelled.
 */
open class CancellationException : Exception()

/**
 * An exception indicating that a user input process has been cancelled.
 */
open class InputCancelledException : Exception()

/**
 * An exception indicating that a user has opted to open an exported bill collection.
 */
open class OpenExportedBillsException : Exception()

/**
 * A wrapper exception indicating that a background task has thrown some throwable. This exception can hold an
 * instruction to exit the application when caught.
 */
open class BackgroundTaskException(cause: Throwable, val shouldExit: Boolean = false) : Exception(cause)