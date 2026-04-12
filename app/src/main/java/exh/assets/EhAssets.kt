package exh.assets

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.collections.List as ____KtList

object EhAssets

@Suppress("ObjectPropertyName", "ktlint:standard:backing-property-naming")
private var __AllAssets: ____KtList<ImageVector>? = null

val EhAssets.AllAssets: ____KtList<ImageVector>
    get() {
        if (__AllAssets != null) {
            return __AllAssets!!
        }
        __AllAssets = listOf(EhLogo, MangadexLogo)
        return __AllAssets!!
    }
