package exh.log

import android.util.Log
import co.touchlab.kermit.Logger

/**
 * Compatibility shim: maps SY's xLog calls to Kermit.
 */
fun Any.xLog(): Logger = Logger.withTag(this::class.java.simpleName)

fun Any.xLogStack(): Logger = Logger.withTag(this::class.java.simpleName)

fun Any.xLogE(log: String) = xLog().e { log }
fun Any.xLogW(log: String) = xLog().w { log }
fun Any.xLogD(log: String) = xLog().d { log }
fun Any.xLogI(log: String) = xLog().i { log }

fun Any.xLogE(log: String, e: Throwable) = xLog().e(e) { log }
fun Any.xLogW(log: String, e: Throwable) = xLog().w(e) { log }
fun Any.xLogD(log: String, e: Throwable) = xLog().d(e) { log }
fun Any.xLogI(log: String, e: Throwable) = xLog().i(e) { log }

fun Any.xLogE(log: Any?) = xLog().e { log?.toString() ?: "null" }
fun Any.xLogW(log: Any?) = xLog().w { log?.toString() ?: "null" }
fun Any.xLogD(log: Any?) = xLog().d { log?.toString() ?: "null" }
fun Any.xLogI(log: Any?) = xLog().i { log?.toString() ?: "null" }
fun Any.xLog(logLevel: LogLevel, log: Any?) = logLevel.logWith(xLog(), log?.toString() ?: "null")

fun Any.xLogD(format: String, vararg args: Any?) = xLog().d { String.format(format, *args) }
fun Any.xLogE(format: String, vararg args: Any?) = xLog().e { String.format(format, *args) }
fun Any.xLogW(format: String, vararg args: Any?) = xLog().w { String.format(format, *args) }
fun Any.xLogI(format: String, vararg args: Any?) = xLog().i { String.format(format, *args) }
fun Any.xLog(logLevel: LogLevel, format: String, vararg args: Any) = logLevel.logWith(xLog(), String.format(format, *args))

fun Any.xLog(logLevel: LogLevel, log: String) = logLevel.logWith(xLog(), log)
fun Any.xLog(logLevel: LogLevel, log: String, e: Throwable) = logLevel.logWith(xLog(), log, e)

fun Any.xLogJson(log: String) = xLog().d { log }
fun Any.xLogXML(log: String) = xLog().d { log }

sealed class LogLevel(val int: Int, val androidLevel: Int) {
    data object None : LogLevel(0, Log.ASSERT)
    data object Error : LogLevel(1, Log.ERROR)
    data object Warn : LogLevel(2, Log.WARN)
    data object Info : LogLevel(3, Log.INFO)
    data object Debug : LogLevel(4, Log.DEBUG)
    data object Verbose : LogLevel(5, Log.VERBOSE)
    data object All : LogLevel(6, Log.VERBOSE)

    val name get() = when (this) {
        None -> "NONE"
        Error -> "ERROR"
        Warn -> "WARN"
        Info -> "INFO"
        Debug -> "DEBUG"
        Verbose -> "VERBOSE"
        All -> "ALL"
    }

    val shortName get() = when (this) {
        None -> "N"
        Error -> "E"
        Warn -> "W"
        Info -> "I"
        Debug -> "D"
        Verbose -> "V"
        All -> "A"
    }

    fun logWith(logger: Logger, msg: String, throwable: Throwable? = null) {
        when (this) {
            Error -> if (throwable != null) logger.e(throwable) { msg } else logger.e { msg }
            Warn -> if (throwable != null) logger.w(throwable) { msg } else logger.w { msg }
            Info -> if (throwable != null) logger.i(throwable) { msg } else logger.i { msg }
            Debug, Verbose, All -> if (throwable != null) logger.d(throwable) { msg } else logger.d { msg }
            None -> { /* no-op */ }
        }
    }

    companion object {
        fun values() = listOf(None, Error, Warn, Info, Debug, Verbose, All)
    }
}

@Deprecated("Use proper throwable function", ReplaceWith("""xLogE("", log)"""))
fun Any.xLogE(log: Throwable) = xLog().e(log) { "" }

@Deprecated("Use proper throwable function", ReplaceWith("""xLogW("", log)"""))
fun Any.xLogW(log: Throwable) = xLog().w(log) { "" }

@Deprecated("Use proper throwable function", ReplaceWith("""xLogD("", log)"""))
fun Any.xLogD(log: Throwable) = xLog().d(log) { "" }

@Deprecated("Use proper throwable function", ReplaceWith("""xLogI("", log)"""))
fun Any.xLogI(log: Throwable) = xLog().i(log) { "" }

@Deprecated("Use proper throwable function", ReplaceWith("""xLog(logLevel, "", log)"""))
fun Any.xLog(logLevel: LogLevel, log: Throwable) = logLevel.logWith(xLog(), "", log)
