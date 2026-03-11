package eu.kanade.tachiyomi.util.system

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.IntentCompat
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.Serializable

fun Uri.toShareIntent(context: Context, type: String = "image/*", message: String? = null): Intent {
    val uri = this

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        val chooserTitle = message ?: context.stringResource(MR.strings.action_share)
        when (uri.scheme) {
            "http", "https" -> {
                putExtra(Intent.EXTRA_TEXT, uri.toString())
                putExtra(Intent.EXTRA_SUBJECT, message ?: uri.toString())
                putExtra(Intent.EXTRA_TITLE, chooserTitle)
                setType("text/plain")
            }
            "content", "file" -> {
                message?.let { putExtra(Intent.EXTRA_TEXT, it) }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
                putExtra(Intent.EXTRA_TITLE, chooserTitle)
                setType(type)
                clipData = ClipData.newRawUri(null, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            else -> {
                putExtra(Intent.EXTRA_TEXT, message ?: uri.toString())
                putExtra(Intent.EXTRA_SUBJECT, chooserTitle)
                putExtra(Intent.EXTRA_TITLE, chooserTitle)
                setType("text/plain")
            }
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }

    return Intent.createChooser(shareIntent, context.stringResource(MR.strings.action_share)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

inline fun <reified T> Intent.getParcelableExtraCompat(name: String): T? {
    return IntentCompat.getParcelableExtra(this, name, T::class.java)
}

inline fun <reified T : Serializable> Intent.getSerializableExtraCompat(name: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(name) as? T
    }
}
