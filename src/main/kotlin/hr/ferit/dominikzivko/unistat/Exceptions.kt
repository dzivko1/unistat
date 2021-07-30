package hr.ferit.dominikzivko.unistat

open class UnexpectedResponseException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

open class NotLoggedInException : Exception()

open class LoginFailedException : Exception {
    val userFriendlyMessage: String?

    constructor() : super() {
        this.userFriendlyMessage = null
    }

    constructor(message: String, userFriendlyMessage: String? = null) : super(message) {
        this.userFriendlyMessage = userFriendlyMessage
    }
}

open class CancellationException : Exception()

open class InputCancelledException : Exception()

open class OpenExportedBillsException : Exception()

open class BackgroundTaskException(cause: Throwable, val shouldExit: Boolean = false) : Exception(cause)