package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Renders novel source icons at the same width, height and shape as manga
 * adaptive launcher icons. Manga icons are drawn by Android as `AdaptiveIconDrawable`,
 * which fills the full ImageView bounds and is clipped by the system's adaptive
 * icon mask; we match that here by drawing the loaded bitmap into the full bitmap
 * area and clipping with `AdaptiveIconDrawable.getIconMask()` on API 26+
 * (rounded-rect fallback below that, since adaptive icons don't exist on those APIs).
 */
class PaddedSourceIconTransformation(
    private val insetFraction: Float = 0f,
    private val cornerRadiusFraction: Float = 0.18f,
) : Transformation() {

    override val cacheKey: String =
        "${PaddedSourceIconTransformation::class.java.name}-i$insetFraction-r$cornerRadiusFraction-v2"

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
        val clipPath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // AdaptiveIconDrawable's mask is authored in a 100×100 viewport.
            val mask = Path(AdaptiveIconDrawable(null, null).iconMask)
            val matrix = Matrix().apply {
                setScale(dst.width() / MASK_VIEWPORT, dst.height() / MASK_VIEWPORT)
                postTranslate(dst.left, dst.top)
            }
            mask.transform(matrix)
            mask
        } else {
            val radius = minOf(dst.width(), dst.height()) * cornerRadiusFraction
            Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) }
        }
        val saved = canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(input, src, dst, paint)
        canvas.restoreToCount(saved)
        return output
    }

    private companion object {
        const val MASK_VIEWPORT = 100f
    }
}
