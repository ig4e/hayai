package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Insets novel source icons inside a transparent border so they match the visual size of
 * manga adaptive launcher icons (Android's 108dp/66dp safe-zone ≈ 19% inset on each side).
 */
class PaddedSourceIconTransformation(
    private val insetFraction: Float = DEFAULT_INSET_FRACTION,
    private val cornerRadiusFraction: Float = DEFAULT_CORNER_RADIUS_FRACTION,
) : Transformation() {

    override val cacheKey: String =
        "${PaddedSourceIconTransformation::class.java.name}-i$insetFraction-r$cornerRadiusFraction"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        val output = createBitmap(w, h, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val insetX = w * insetFraction
        val insetY = h * insetFraction
        val dst = RectF(insetX, insetY, w - insetX, h - insetY)
        val src = Rect(0, 0, w, h)
        val saved = canvas.save()
        val radius = minOf(dst.width(), dst.height()) * cornerRadiusFraction
        canvas.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
        canvas.drawBitmap(input, src, dst, paint)
        canvas.restoreToCount(saved)
        return output
    }

    companion object {
        private const val DEFAULT_INSET_FRACTION = 0.19f
        private const val DEFAULT_CORNER_RADIUS_FRACTION = 0.18f
    }
}
