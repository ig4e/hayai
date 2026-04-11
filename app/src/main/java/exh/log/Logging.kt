package exh.log

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

fun Any.xLogD(format: String, vararg args: Any?) = xLog().d { String.format(format, *args) }
fun Any.xLogE(log: Any?) = xLog().e { log?.toString() ?: "null" }
fun Any.xLogW(log: Any?) = xLog().w { log?.toString() ?: "null" }

/**
 * Stub for EH network logging injection. In Hayai we use Kermit instead.
 */
fun okhttp3.OkHttpClient.Builder.maybeInjectEHLogger(): okhttp3.OkHttpClient.Builder = this
