package org.example.com.github.kodakodapa.kingfractal.utils

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


// Image data implementation
data class ImageData(
    val width: Int,
    val height: Int,
    val pixels: ByteArray
) : OpenCLData {

    override fun toByteArray(): ByteArray = pixels
    override fun getBufferSize(): Long = pixels.size.toLong()

    fun toBufferedImage(): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = (y * width + x) * 3
                val r = (pixels[index].toInt() and 0xFF)
                val g = (pixels[index + 1].toInt() and 0xFF)
                val b = (pixels[index + 2].toInt() and 0xFF)
                val rgb = (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, rgb)
            }
        }
        return image
    }

    fun saveAsPng(filename: String) {
        try {
            val bufferedImage = toBufferedImage()
            ImageIO.write(bufferedImage, "PNG", File(filename))
            println("Saved PNG: $filename (${File(filename).length()} bytes)")
        } catch (e: Exception) {
            println("Error saving PNG: ${e.message}")
        }
    }

    companion object {
        fun fromDimensions(width: Int, height: Int): ImageData {
            val pixels = ByteArray(width * height * 3) // RGB
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
