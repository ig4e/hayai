package app.cash.quickjs

import com.dokar.quickjs.QuickJsException as DokarQuickJsException
import com.dokar.quickjs.QuickJs as DokarQuickJs
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Drop-in replacement for `app.cash.quickjs.QuickJs` backed by dokar3/quickjs-kt.
 *
 * Why: installed manga extensions (Mangago, MangaHere, Readcomiconline, the keiyoushi
 * Synchrony deobfuscator, etc.) are precompiled against this class. The original Cash
 * App library is archived, and shipping its native `libquickjs.so` alongside dokar3's
 * collides — they wrap the same upstream C engine and both use the same .so name.
 * This shim keeps a single native lib (dokar3's) and routes the legacy API to it.
 *
 * Threading: each instance owns a dedicated single-thread executor with an 8 MB stack
 * (recursive parsers in JSFuck/Synchrony exceed the default 1 MB). The dispatcher is
 * what dokar3 needs to honor its thread-affinity contract. Synchronous methods bridge
 * to the dispatcher via `runBlocking`, matching Cash App's blocking semantics.
 */
class QuickJs private constructor(
    private val executor: ExecutorService,
    private val delegate: DokarQuickJs,
) : Closeable {

    @Volatile private var closed = false

    fun evaluate(script: String): Any? = evaluate(script, "?")

    fun evaluate(script: String, fileName: String): Any? {
        checkOpen()
        return runOnEngine {
            try {
                delegate.evaluate<Any?>(script, fileName).toCashAppShape()
            } catch (e: DokarQuickJsException) {
                throw QuickJsException(e.message ?: "evaluate failed").initCauseChained(e)
            }
        }
    }

    fun compile(sourceCode: String, fileName: String): ByteArray {
        checkOpen()
        return runOnEngine {
            try {
                delegate.compile(sourceCode, fileName)
            } catch (e: DokarQuickJsException) {
                throw QuickJsException(e.message ?: "compile failed").initCauseChained(e)
            }
        }
    }

    fun execute(bytecode: ByteArray): Any? {
        checkOpen()
        return runOnEngine {
            try {
                delegate.evaluate<Any?>(bytecode).toCashAppShape()
            } catch (e: DokarQuickJsException) {
                throw QuickJsException(e.message ?: "execute failed").initCauseChained(e)
            }
        }
    }

    /**
     * Cash App's API lets you expose a Kotlin/Java object to JS by binding a class's
     * methods as JS-callable globals. dokar3 has a different binding model (closure-
     * based, not interface-based); shimming it would require runtime proxy generation
     * to map every interface method onto dokar3's `defineBinding` lambdas. No extension
     * in the keiyoushi survey uses it, so we stub it with a clear failure rather than
     * pretend support and break in subtle ways.
     */
    fun <T : Any> set(name: String, type: Class<T>, instance: T) {
        throw UnsupportedOperationException(
            "QuickJs.set(name, type, instance) is not supported by the dokar3 shim. " +
                "If you hit this, the extension uses an uncommon Cash-App-specific binding API; " +
                "file an issue with the extension name.",
        )
    }

    /** Same situation as [set] — interface-proxy generation isn't shimmed. */
    fun <T : Any> get(name: String, type: Class<T>): T {
        throw UnsupportedOperationException(
            "QuickJs.get(name, type) is not supported by the dokar3 shim. " +
                "If you hit this, the extension uses an uncommon Cash-App-specific binding API; " +
                "file an issue with the extension name.",
        )
    }

    // dokar3 doesn't expose limit/usage knobs. Cash App callers in the wild don't use
    // these (none in the keiyoushi survey); accept-and-ignore preserves ABI without
    // pretending we enforce a limit we can't.
    fun getMemoryLimit(): Long = -1L
    fun setMemoryLimit(limit: Long) = Unit
    fun getMemoryUsage(): Long = -1L
    fun getGcThreshold(): Long = -1L
    fun setGcThreshold(gcThreshold: Long) = Unit

    override fun close() {
        if (closed) return
        closed = true
        // Close the engine on its own thread (dokar3 enforces thread affinity), then
        // tear the executor down so the 8 MB stack reservation is released.
        try {
            runOnEngine { delegate.close() }
        } catch (_: Throwable) {
        }
        executor.shutdown()
    }

    private inline fun <T> runOnEngine(crossinline block: suspend () -> T): T {
        return runBlocking(dispatcher) { block() }
    }

    private fun checkOpen() {
        if (closed) throw IllegalStateException("QuickJs is closed")
    }

    private val dispatcher = executor.asCoroutineDispatcher()

    companion object {
        @JvmStatic
        fun create(): QuickJs {
            val executor = Executors.newSingleThreadExecutor { runnable ->
                Thread(null, runnable, "quickjs-shim", 8L * 1024 * 1024)
            }
            val dispatcher = executor.asCoroutineDispatcher()
            // Create the engine on the dispatcher thread so its native context is owned
            // by the thread we'll use for every subsequent call.
            val engine = runBlocking(dispatcher) { DokarQuickJs.create(dispatcher) }
            return QuickJs(executor, engine)
        }
    }
}

private fun <T : Throwable> T.initCauseChained(cause: Throwable): T = apply { initCause(cause) }

/**
 * Normalize dokar3's return shape to what Cash App's QuickJs produced. Two
 * divergences matter:
 *
 *   1. JS arrays — dokar3 returns `List<Any?>`, Cash App returned `Object[]`.
 *      Comicabc and friends cast directly to `Array<*>`, which would otherwise
 *      `ClassCastException`. Converted recursively so nested arrays normalize too.
 *
 *   2. JS numbers — JS has a single Number type, but dokar3 splits the JVM mapping
 *      into `Long` (for integral values) and `Double` (for fractional). Cash App
 *      returned `Double` for everything. We mirror that so `evaluate(...) as Double`
 *      keeps working for extensions that integer-arithmetic'd in JS.
 *
 * Maps, strings, booleans, and ByteArrays pass through unchanged — Cash App's
 * mapping for those matched dokar3's.
 */
private fun Any?.toCashAppShape(): Any? = when (this) {
    is List<*> -> Array<Any?>(size) { i -> this[i].toCashAppShape() }
    is Long -> this.toDouble()
    is Int -> this.toDouble()
    is Short -> this.toDouble()
    is Byte -> this.toDouble()
    is Float -> this.toDouble()
    else -> this
}

