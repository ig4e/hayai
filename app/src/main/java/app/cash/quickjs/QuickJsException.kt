package app.cash.quickjs

/** Drop-in replacement for the legacy Cash App exception type. */
class QuickJsException(message: String) : RuntimeException(message)
