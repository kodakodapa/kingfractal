package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.ARGBPalette
import com.github.kodakodapa.kingfractal.colors.ARGBRainbowPalette
import com.github.kodakodapa.kingfractal.utils.OpenCLData
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

const val ARGB_CHANNELS = 4

// Image data implementation
data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) : OpenCLData {


    override fun toByteArray(): ByteArray = pixels
    override fun getBufferSize(): Long = pixels.size.toLong()

    fun toBufferedImage(palette: ARGBPalette?): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val actualPalette = palette ?: ARGBRainbowPalette()
        val colorMatrix = actualPalette.generateColorMatrix(256).toMatrix()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * ARGB_CHANNELS

                // Get the normalized iteration value (0-255)
                val iterationValue = pixels[index].toInt() and 0xFF

                // Apply enhanced color mapping for better visual quality
                val enhancedIndex = enhanceColorIndex(iterationValue)

                val a = colorMatrix[enhancedIndex][0]
                val r = colorMatrix[enhancedIndex][1]
                val g = colorMatrix[enhancedIndex][2]
                val b = colorMatrix[enhancedIndex][3]
                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, argb)
            }
        }
        return image
    }

    /**
     * Enhanced color index mapping for better visual quality
     * Applies logarithmic scaling to emphasize detail in interesting regions
     */
    private fun enhanceColorIndex(value: Int): Int {
        if (value == 255) return 255 // Handle max iterations (inside set)

        // Apply logarithmic scaling to spread out low iteration values
        val normalizedValue = value / 255.0
        val logValue = kotlin.math.ln(1.0 + normalizedValue * 9.0) / kotlin.math.ln(10.0)
        val enhancedValue = (logValue * 255.0).toInt()

        return enhancedValue.coerceIn(0, 255)
    }

    fun saveAsPng(filename: String, palette: ARGBPalette?) {
        try {
            val bufferedImage = toBufferedImage(palette)
            ImageIO.write(bufferedImage, "PNG", File(filename))
            println("Saved PNG: $filename (${File(filename).length()} bytes)")
        } catch (e: Exception) {
            println("Error saving PNG: ${e.message}")
        }
    }

    companion object {
        fun fromDimensions(width: Int, height: Int): ImageData {
            val pixels = ByteArray(width * height * ARGB_CHANNELS) // RGB
            return ImageData(width, height, pixels)
        }

        fun fromByteArray(width: Int, height: Int, bytes: ByteArray): ImageData {
            return ImageData(width, height, bytes)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageData

        if (width != other.width) return false
        if (height != other.height) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
