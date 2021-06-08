package hr.ferit.dominikzivko.unistat

open class UnexpectedResponseException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
}

open class LoginFailedException : Exception {
    val userFriendlyMessage: String?

    constructor() : super() {
        this.userFriendlyMessage = null
    }
    constructor(message: String, userFriendlyMessage: String? = null) : super(message) {
        this.userFriendlyMessage = userFriendlyMessage
    }
}

open class InputCanceledException : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
}