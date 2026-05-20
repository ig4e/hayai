package exh.md

import yokai.util.koin.get
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.source.SourceManager
import exh.md.utils.MdUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity that handles MangaDex OAuth login callbacks.
 *
 * Registered in AndroidManifest for the hayai://mangadex-auth redirect URI.
 * When MangaDex completes OAuth, it redirects back here with an authorization code.
 * This activity extracts the code, performs the token exchange via the MangaDex source,
 * and finishes.
 */
class MangaDexLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data
        val code = uri?.getQueryParameter("code")

        lifecycleScope.launch(Dispatchers.IO) {
            val mangaDex = MdUtil.getEnabledMangaDex(get<SourceManager>())
            if (code != null && mangaDex != null) {
                mangaDex.login(code)
            } else if (mangaDex != null) {
                // No code means the auth was cancelled or failed; log out to clean state
                mangaDex.logout()
            }
            withContext(Dispatchers.Main) {
                finish()
            }
        }
    }
}
