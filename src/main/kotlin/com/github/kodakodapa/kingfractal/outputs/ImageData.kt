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

    fun toBufferedImage(palette: ARGBPalette?, useHistogramEqualization: Boolean = true): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val actualPalette = palette ?: ARGBRainbowPalette()
        val colorMatrix = actualPalette.generateColorMatrix(256).toMatrix()
        val equalizedValues = calculateLogarithmicHistogramEqualization()

        // Calculate color mapping based on selected method
        val colorMappingFunction: (Int) -> Int = if (useHistogramEqualization) {
            { iterationValue -> equalizedValues[iterationValue] }
        } else {
            // Use simple logarithmic enhancement for comparison
            { iterationValue -> enhanceColorIndexSimple(iterationValue) }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * ARGB_CHANNELS

                // Get the normalized iteration value (0-255)
                val iterationValue = pixels[index].toInt() and 0xFF

                // Apply selected color mapping
                val mappedIndex = colorMappingFunction(iterationValue)

                val a = colorMatrix[mappedIndex][0]
                val r = colorMatrix[mappedIndex][1]
                val g = colorMatrix[mappedIndex][2]
                val b = colorMatrix[mappedIndex][3]
                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, argb)
            }
        }
        return image
    }

    /**
     * Calculates logarithmic histogram equalization for better fractal visualization
     * This enhances contrast by redistributing iteration values based on their frequency
     */
    private fun calculateLogarithmicHistogramEqualization(): IntArray {
        // Build histogram of iteration values
        val histogram = IntArray(256) { 0 }

        for (i in pixels.indices step ARGB_CHANNELS) {
            val iterationValue = pixels[i].toInt() and 0xFF
            histogram[iterationValue]++
        }

        // Calculate cumulative distribution
        val cdf = DoubleArray(256)
        cdf[0] = histogram[0].toDouble()
        for (i in 1 until 256) {
            cdf[i] = cdf[i - 1] + histogram[i]
        }

        // Find min non-zero CDF value for proper normalization
        val cdfMin = cdf.find { it > 0.0 } ?: 1.0
        val totalPixels = (width * height).toDouble()

        // Apply logarithmic histogram equalization
        val equalizedValues = IntArray(256)

        for (i in 0 until 256) {
            if (histogram[i] == 0) {
                // No pixels with this value, keep original
                equalizedValues[i] = i
            } else {
                // Apply logarithmic equalization formula
                val normalizedCdf = (cdf[i] - cdfMin) / (totalPixels - cdfMin)

                // Apply logarithmic scaling for better visual distribution
                val logValue = if (normalizedCdf > 0.0) {
                    kotlin.math.ln(1.0 + normalizedCdf * (kotlin.math.E - 1.0))
                } else {
                    0.0
                }

                equalizedValues[i] = (logValue * 255.0).toInt().coerceIn(0, 255)
            }
        }

        // Ensure max iterations (inside set) map to a distinct color
        if (histogram[255] > 0) {
            equalizedValues[255] = 255
        }

        return equalizedValues
    }

    /**
     * Simple logarithmic color enhancement (legacy method for comparison)
     */
    private fun enhanceColorIndexSimple(value: Int): Int {
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
