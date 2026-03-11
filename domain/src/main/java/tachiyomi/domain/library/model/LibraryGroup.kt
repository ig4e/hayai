package tachiyomi.domain.library.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

object LibraryGroup {

    const val BY_DEFAULT = 0
    const val BY_SOURCE = 1
    const val BY_STATUS = 2
    const val BY_TRACK_STATUS = 3
    const val UNGROUPED = 4
    const val BY_AUTHOR = 5
    const val BY_LANGUAGE = 6
    const val BY_TAG = 7

    fun groupTypeStringRes(type: Int, hasCategories: Boolean = true): StringResource {
        return when (type) {
            BY_STATUS -> MR.strings.status
            BY_SOURCE -> MR.strings.label_sources
            BY_TRACK_STATUS -> SYMR.strings.tracking_status
            UNGROUPED -> SYMR.strings.ungrouped
            BY_AUTHOR -> MR.strings.author
            BY_LANGUAGE -> MR.strings.language
            BY_TAG -> MR.strings.label_tags
            else -> if (hasCategories) MR.strings.categories else SYMR.strings.ungrouped
        }
    }
}
