package eu.kanade.tachiyomi.extension.installer

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.util.system.getUriSize
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import java.io.BufferedReader
import java.io.InputStream
import java.lang.reflect.Method
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import yokai.i18n.MR
import yokai.util.lang.getString

class ShizukuInstaller(
    context: Context,
    finishedQueue: (Installer) -> Unit,
) : Installer(context, finishedQueue) {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val shizukuDeadListener = Shizuku.OnBinderDeadListener {
        Logger.d { "Shizuku was killed prematurely" }
        finishedQueue(this)
    }

    private val shizukuPermissionListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    ready = true
                    checkQueue()
                } else {
                    finishedQueue(this@ShizukuInstaller)
                }
                Shizuku.removeRequestPermissionResultListener(this)
            }
        }
    }

    override var ready = false

    private val newProcess: Method

    init {
        Shizuku.addBinderDeadListener(shizukuDeadListener)
        require(Shizuku.pingBinder() && context.isShizukuInstalled) {
            finishedQueue(this)
            context.getString(MR.strings.ext_installer_shizuku_stopped)
        }
        ready = if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            false
        }
        newProcess = Shizuku::class.java
            .getDeclaredMethod("newProcess", Array<out String>::class.java, Array<out String>::class.java, String::class.java)
        newProcess.isAccessible = true
    }

    override fun processEntry(entry: Entry) {
        super.processEntry(entry)
        ioScope.launch {
            var sessionId: String? = null
            try {
                val size = context.getUriSize(entry.uri) ?: throw IllegalStateException()
                context.contentResolver.openInputStream(entry.uri)!!.use {
                    val createCommand = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val userId = Process.myUserHandle().hashCode()
                        "pm install-create --user $userId -r -i ${context.packageName} -S $size"
                    } else {
                        "pm install-create -r -i ${context.packageName} -S $size"
                    }
                    val createResult = exec(createCommand)
                    sessionId = SESSION_ID_REGEX.find(createResult.out)?.value
                        ?: throw RuntimeException("Failed to create install session")

                    val writeResult = exec("pm install-write -S $size $sessionId base -", it)
                    if (writeResult.resultCode != 0) {
                        throw RuntimeException("Failed to write APK to session $sessionId")
                    }

                    val commitResult = exec("pm install-commit $sessionId")
                    if (commitResult.resultCode != 0) {
                        throw RuntimeException("Failed to commit install session $sessionId")
                    }

                    continueQueue(true)
                }
            } catch (e: Exception) {
                Logger.e(e) { "Failed to install extension ${entry.downloadId} ${entry.uri}" }
                if (sessionId != null) {
                    exec("pm install-abandon $sessionId")
                }
                continueQueue(false)
            }
        }
    }

    // Don't cancel if entry is already started installing
    override fun cancelEntry(entry: Entry): Boolean = getActiveEntry() != entry

    override fun onDestroy() {
        Shizuku.removeBinderDeadListener(shizukuDeadListener)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        ioScope.cancel()
        super.onDestroy()
    }

    private fun exec(command: String, stdin: InputStream? = null): ShellResult {
        val process = newProcess.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
        if (stdin != null) {
            process.outputStream.use { stdin.copyTo(it) }
        }
        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val resultCode = process.waitFor()
        return ShellResult(resultCode, output)
    }

    private data class ShellResult(val resultCode: Int, val out: String)

    companion object {
        const val shizukuPkgName = "moe.shizuku.privileged.api"
        const val downloadLink = "https://shizuku.rikka.app/download"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 14045
        // Closing `]` inside the lookahead must be escaped — Android's ICU regex parser rejects bare `]`.
        private val SESSION_ID_REGEX = Regex("(?<=\\[).+?(?=\\])")
        fun isShizukuRunning(): Boolean {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }
}
