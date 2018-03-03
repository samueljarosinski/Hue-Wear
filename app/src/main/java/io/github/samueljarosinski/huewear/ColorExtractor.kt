package io.github.samueljarosinski.huewear

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.ColorInt
import android.widget.ImageView

typealias OnColorExtractedListener = (Int) -> Unit

class ColorExtractor(
    private val paletteView: ImageView,
    private val minUpdateDelay: Int,
    private val onColorExtracted: OnColorExtractedListener
) {

    private val paletteDrawable = paletteView.drawable as BitmapDrawable
    private var previousColor: Int = Color.WHITE
    private var lastUpdateTime: Long = 0

    fun extract(position: Position) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastUpdateTime >= minUpdateDelay) {
            val color = getColorAt(position)

            if (color != previousColor) {
                onColorExtracted(color)

                previousColor = color
                lastUpdateTime = currentTime
            }
        }
    }

    @ColorInt
    private fun getColorAt(position: Position): Int {
        val invertMatrix = Matrix()
        paletteView.imageMatrix.invert(invertMatrix)

        val mappedPoints = floatArrayOf(position.first, position.second)
        invertMatrix.mapPoints(mappedPoints)

        if (mappedPoints[0] > 0 && mappedPoints[1] > 0 &&
            mappedPoints[0] < paletteDrawable.intrinsicWidth && mappedPoints[1] < paletteDrawable.intrinsicHeight) {

            return paletteDrawable.bitmap.getPixel(mappedPoints[0].toInt(), mappedPoints[1].toInt())
        }

        return Color.WHITE
    }
}
