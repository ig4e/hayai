package eu.kanade.tachiyomi.source

import yokai.util.koin.get
import android.app.Application
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID

fun Source.includeLangInName(enabledLanguages: Set<String>, extensionManager: ExtensionManager? = null): Boolean {
    val httpSource = this as? HttpSource ?: return false
    val extManager = extensionManager ?: get()
    val allExt = httpSource.getExtension(extManager)?.lang == "all"
    val onlyAll = httpSource.extOnlyHasAllLanguage(extManager)
    val isMultiLingual = enabledLanguages.filterNot { it == "all" }.size > 1
    return (isMultiLingual && allExt) || (lang == "all" && !onlyAll)
}

fun Source.nameBasedOnEnabledLanguages(enabledLanguages: Set<String>, extensionManager: ExtensionManager? = null): String {
    return if (this is HttpSource && includeLangInName(enabledLanguages, extensionManager)) toString() else name
}

fun Source.icon(): Drawable? {
    when (id) {
        EH_SOURCE_ID -> return ContextCompat.getDrawable(get<Application>(), R.mipmap.ic_ehentai_source)
        EXH_SOURCE_ID -> return ContextCompat.getDrawable(get<Application>(), R.mipmap.ic_exhentai_source)
    }
    return get<ExtensionManager>().getAppIconForSource(this)
}

fun Source.pkgName() = get<ExtensionManager>().getPackageName(this.id)
fun HttpSource.getExtension(extensionManager: ExtensionManager? = null): Extension.Installed? =
    (extensionManager ?: get()).installedExtensionsFlow.value.find { it.sources.contains(this) }

fun HttpSource.extOnlyHasAllLanguage(extensionManager: ExtensionManager? = null) =
    getExtension(extensionManager)?.sources?.all { it.lang == "all" } ?: true
