package exh.eh

import android.util.SparseArray
import androidx.core.util.AtomicFile
import androidx.core.util.forEach
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import kotlin.concurrent.thread

/**
 * In memory Int -> Obj lookup table implementation that
 * automatically persists itself to disk atomically and asynchronously.
 *
 * Thread safe
 *
 * @author nulldev
 */
class MemAutoFlushingLookupTable<T>(
    file: File,
    private val serializer: EntrySerializer<T>,
    private val debounceTimeMs: Long = 3000,
) : CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()), Closeable {

    private val table = SparseArray<T>(INITIAL_SIZE)
    private val mutex = Mutex(true)

    // Used to debounce
    @Volatile
    private var writeCounter = Long.MIN_VALUE

    @Volatile
    private var flushed = true

    private val atomicFile = AtomicFile(file)

    private val shutdownHook = thread(start = false) {
        if (!flushed) writeSynchronously()
    }

    init {
        initialLoad()

        Runtime.getRuntime().addShutdownHook(shutdownHook)
    }

    private fun BufferedSource.requireBytes(targetArray: ByteArray, byteCount: Int): Boolean {
        var readIter = 0
        while (true) {
            val readThisIter = read(targetArray, readIter, byteCount - readIter)
            if (readThisIter <= 0) return false // No more data to read
            readIter += readThisIter
            if (readIter == byteCount) return true
        }
    }

    private fun initialLoad() {
        launch {
            try {
                atomicFile.openRead().source().buffer().use { input ->
                    val bb = ByteBuffer.allocate(8)

                    while (true) {
                        if (!input.requireBytes(bb.array(), 8)) break
                        val k = bb.getInt(0)
                        val size = bb.getInt(4)
                        val strBArr = ByteArray(size)
                        if (!input.requireBytes(strBArr, size)) break
                        table.put(k, serializer.read(strBArr.decodeToString()))
                    }
                }
            } catch (e: FileNotFoundException) {
                Logger.d("MemAutoFlushingLookupTable") { "Lookup table not found!" }
                // Ignored
            }

            mutex.unlock()
        }
    }

    private fun tryWrite() {
        val id = ++writeCounter
        flushed = false
        launch {
            delay(debounceTimeMs)
            if (id != writeCounter) return@launch

            mutex.withLock {
                // Second check inside of mutex to prevent dupe writes
                if (id != writeCounter) return@launch
                withContext(NonCancellable) {
                    writeSynchronously()

                    // Yes there is a race here, no it's isn't critical
                    if (id == writeCounter) flushed = true
                }
            }
        }
    }

    private fun writeSynchronously() {
        val bb = ByteBuffer.allocate(ENTRY_SIZE_BYTES)

        val fos = atomicFile.startWrite()
        try {
            val out = fos.sink().buffer()
            table.forEach { key, value ->
                val v = serializer.write(value).encodeToByteArray()
                bb.putInt(0, key)
                bb.putInt(4, v.size)
                out.write(bb.array())
                out.write(v)
            }
            out.flush()
            atomicFile.finishWrite(fos)
        } catch (t: Throwable) {
            atomicFile.failWrite(fos)
            throw t
        }
    }

    suspend fun put(key: Int, value: T) {
        mutex.withLock { table.put(key, value) }
        tryWrite()
    }

    suspend fun get(key: Int): T? {
        return mutex.withLock { table.get(key) }
    }

    suspend fun size(): Int {
        return mutex.withLock { table.size() }
    }

    override fun close() {
        runBlocking { coroutineContext.job.cancelAndJoin() }
        Runtime.getRuntime().removeShutdownHook(shutdownHook)
    }

    interface EntrySerializer<T> {
        /**
         * Serialize an entry as a String.
         */
        fun write(entry: T): String

        /**
         * Read an entry from a String.
         */
        fun read(string: String): T
    }

    companion object {
        private const val INITIAL_SIZE = 1000
        private const val ENTRY_SIZE_BYTES = 8
    }
}
