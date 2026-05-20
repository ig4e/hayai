package eu.kanade.tachiyomi.source

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import org.koin.core.context.GlobalContext

interface ConfigurableSource : Source {

    /**
     * Gets instance of [SharedPreferences] scoped to the specific source.
     *
     * @since extensions-lib 1.5
     */
    fun getSourcePreferences(): SharedPreferences =
        GlobalContext.get().get<Application>().getSharedPreferences(preferenceKey(), Context.MODE_PRIVATE)

    fun setupPreferenceScreen(screen: PreferenceScreen)
}

// TODO: use getSourcePreferences once all extensions are on ext-lib 1.5
fun ConfigurableSource.sourcePreferences(): SharedPreferences =
    GlobalContext.get().get<Application>().getSharedPreferences(preferenceKey(), Context.MODE_PRIVATE)

fun sourcePreferences(key: String): SharedPreferences =
    GlobalContext.get().get<Application>().getSharedPreferences(key, Context.MODE_PRIVATE)
