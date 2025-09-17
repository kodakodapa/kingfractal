package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.ARGBFirePalette
import com.github.kodakodapa.kingfractal.colors.ARGBPalette
import com.github.kodakodapa.kingfractal.utils.OpenCLData
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

const val RGB_CHANNELS = 3


// Image data implementation
data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) : OpenCLData {


    override fun toByteArray(): ByteArray = pixels
    override fun getBufferSize(): Long = pixels.size.toLong()

    fun toBufferedImage(palette: ARGBPalette?): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val actualPalette = palette ?: ARGBFirePalette(enableSmoke = true)
        val colorMatrix = actualPalette.generateColorMatrix(256).toMatrix()
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * RGB_CHANNELS
                val r = colorMatrix[(pixels[index].toInt() and 0xFF -1 )][0]
                val g = colorMatrix[(pixels[index + 1].toInt() and 0xFF -1)][1]
                val b = colorMatrix[(pixels[index + 2].toInt() and 0xFF-1)][2]
                val rgb = (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, rgb)
            }
        }
        return image
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
            val pixels = ByteArray(width * height * RGB_CHANNELS) // RGB
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
