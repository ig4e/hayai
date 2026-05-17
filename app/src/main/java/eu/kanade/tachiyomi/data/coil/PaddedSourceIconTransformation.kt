package eu.kanade.tachiyomi.data.coil

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import coil3.size.Size
import coil3.transform.Transformation

/** Masks novel source icons to a rounded square with a fixed 12% corner radius. */
class PaddedSourceIconTransformation(
    private val cornerRadiusFraction: Float = DEFAULT_CORNER_RADIUS_FRACTION,
) : Transformation() {

    override val cacheKey: String = "${PaddedSourceIconTransformation::class.java.name}-r$cornerRadiusFraction"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val w = input.width
        val h = input.height
        val output = createBitmap(w, h, input.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = minOf(w, h) * cornerRadiusFraction
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), radius, radius, paint)
        return output
    }

    companion object {
        private const val DEFAULT_CORNER_RADIUS_FRACTION = 0.06f
    }
}
