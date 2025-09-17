package com.github.kodakodapa.kingfractal.utils

import com.github.kodakodapa.kingfractal.outputs.ImageData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.awt.image.BufferedImage

const val RGB_CHANNELS = 3

class ImageDataTest {

    @Test
    fun `should create ImageData with correct dimensions`() {
        val width = 100
        val height = 200
        val pixels = ByteArray(width * height * RGB_CHANNELS)

        val imageData = ImageData(width, height, pixels)

        assertEquals(width, imageData.width)
        assertEquals(height, imageData.height)
        assertEquals(pixels.size, imageData.pixels.size)
    }

    @Test
    fun `should create ImageData from dimensions`() {
        val width = 50
        val height = 75

        val imageData = ImageData.fromDimensions(width, height)

        assertEquals(width, imageData.width)
        assertEquals(height, imageData.height)
        assertEquals(width * height * RGB_CHANNELS, imageData.pixels.size)
    }

    @Test
    fun `should return correct buffer size`() {
        val imageData = ImageData.fromDimensions(10, 10)
        val expectedSize = 10 * 10 * RGB_CHANNELS

        assertEquals(expectedSize.toLong(), imageData.getBufferSize())
    }

    @Test
    fun `should convert to byte array`() {
        val pixels = byteArrayOf(1, 2, 3, 4, 5, 6)
        val imageData = ImageData(2, 1, pixels)

        val result = imageData.toByteArray()

        assertArrayEquals(pixels, result)
    }

    @Test
    fun `should create BufferedImage with correct dimensions`() {
        val width = 10
        val height = 15
        val imageData = ImageData.fromDimensions(width, height)

        val bufferedImage = imageData.toBufferedImage(null)

        assertEquals(width, bufferedImage.width)
        assertEquals(height, bufferedImage.height)
        assertEquals(BufferedImage.TYPE_INT_RGB, bufferedImage.type)
    }

    @Test
    fun `should handle color conversion correctly`() {
        val pixels = byteArrayOf(
            255.toByte(), 0, 0,     // Red pixel
            0, 255.toByte(), 0,     // Green pixel
            0, 0, 255.toByte(),     // Blue pixel
            128.toByte(), 128.toByte(), 128.toByte() // Gray pixel
        )
        val imageData = ImageData(2, 2, pixels)

        val bufferedImage = imageData.toBufferedImage(null)

        // Check red pixel (0,0)
        val redPixel = bufferedImage.getRGB(0, 0)
        assertEquals(0xFF0000, redPixel and 0xFFFFFF)

        // Check green pixel (1,0)
        val greenPixel = bufferedImage.getRGB(1, 0)
        assertEquals(0x00FF00, greenPixel and 0xFFFFFF)
    }
}
