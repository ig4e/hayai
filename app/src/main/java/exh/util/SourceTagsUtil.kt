package exh.util

import androidx.core.graphics.toColorInt
import dev.icerock.moko.resources.StringResource
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import yokai.i18n.MR
import java.util.Locale

object SourceTagsUtil {
    fun getWrappedTag(
        sourceId: Long?,
        namespace: String? = null,
        tag: String? = null,
        fullTag: String? = null,
    ): String? {
        return if (
            sourceId == EXH_SOURCE_ID ||
            sourceId == EH_SOURCE_ID
        ) {
            val parsed = when {
                fullTag != null -> parseTag(fullTag)
                namespace != null && tag != null -> RaisedTag(namespace, tag, TAG_TYPE_DEFAULT)
                else -> null
            }
            if (parsed?.namespace != null) {
                wrapTag(parsed.namespace!!, parsed.name.substringBefore('|').trim())
            } else {
                null
            }
        } else {
            null
        }
    }

    private fun wrapTag(namespace: String, tag: String) = if (tag.contains(spaceRegex)) {
        "$namespace:\"$tag$\""
    } else {
        "$namespace:$tag$"
    }

    fun parseTag(tag: String) = RaisedTag(
        if (tag.startsWith("-")) {
            tag.substringAfter("-")
        } else {
            tag
        }.substringBefore(':', missingDelimiterValue = "").trimOrNull(),
        tag.substringAfter(':', missingDelimiterValue = tag).trim(),
        if (tag.startsWith("-")) TAG_TYPE_EXCLUDE else TAG_TYPE_DEFAULT,
    )

    private const val TAG_TYPE_EXCLUDE = 69 // why not

    /**
     * Genre colors for E-Hentai categories.
     * These are kept in sync with [exh.ui.metadata.MetadataUIUtil.getGenreAndColour].
     */
    enum class GenreColor(val color: Int) {
        DOUJINSHI_COLOR("#f44336"),
        MANGA_COLOR("#ff9800"),
        ARTIST_CG_COLOR("#fbc02d"),
        GAME_CG_COLOR("#4caf50"),
        WESTERN_COLOR("#8bc34a"),
        NON_H_COLOR("#2196f3"),
        IMAGE_SET_COLOR("#3f51b5"),
        COSPLAY_COLOR("#9c27b0"),
        ASIAN_PORN_COLOR("#9575cd"),
        MISC_COLOR("#795548"),
        ;

        constructor(color: String) : this(color.toColorInt())
    }

    /**
     * Map an EH category token (as returned by EHentai's parser) to its color and label.
     * Tokens are the lowercased, no-space form: "doujinshi", "manga", "imageset", "artistcg",
     * "gamecg", "western", "non-h", "cosplay", "asianporn", "misc".
     */
    fun getEhCategoryDisplay(token: String?): Pair<GenreColor, StringResource>? = when (token) {
        "doujinshi" -> GenreColor.DOUJINSHI_COLOR to MR.strings.doujinshi
        "manga" -> GenreColor.MANGA_COLOR to MR.strings.manga
        "artistcg" -> GenreColor.ARTIST_CG_COLOR to MR.strings.artist_cg
        "gamecg" -> GenreColor.GAME_CG_COLOR to MR.strings.game_cg
        "western" -> GenreColor.WESTERN_COLOR to MR.strings.western
        "non-h" -> GenreColor.NON_H_COLOR to MR.strings.non_h
        "imageset" -> GenreColor.IMAGE_SET_COLOR to MR.strings.image_set
        "cosplay" -> GenreColor.COSPLAY_COLOR to MR.strings.cosplay
        "asianporn" -> GenreColor.ASIAN_PORN_COLOR to MR.strings.asian_porn
        "misc" -> GenreColor.MISC_COLOR to MR.strings.misc
        else -> null
    }

    fun getLocaleSourceUtil(language: String?) = when (language) {
        "english", "eng" -> Locale.forLanguageTag("en")
        "chinese" -> Locale.forLanguageTag("zh")
        "spanish" -> Locale.forLanguageTag("es")
        "korean" -> Locale.forLanguageTag("ko")
        "russian" -> Locale.forLanguageTag("ru")
        "french" -> Locale.forLanguageTag("fr")
        "portuguese" -> Locale.forLanguageTag("pt")
        "thai" -> Locale.forLanguageTag("th")
        "german" -> Locale.forLanguageTag("de")
        "italian" -> Locale.forLanguageTag("it")
        "vietnamese" -> Locale.forLanguageTag("vi")
        "polish" -> Locale.forLanguageTag("pl")
        "hungarian" -> Locale.forLanguageTag("hu")
        "dutch" -> Locale.forLanguageTag("nl")
        else -> null
    }

    private const val TAG_TYPE_DEFAULT = 1

    private val spaceRegex = "\\s".toRegex()
}
