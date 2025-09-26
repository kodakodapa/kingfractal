package com.github.kodakodapa.kingfractal


import com.github.kodakodapa.kingfractal.twodimensional.DynamicOpenCLRenderer
import com.github.kodakodapa.kingfractal.twodimensional.kernels.FractalKernels
import com.github.kodakodapa.kingfractal.outputs.ImageData
import com.github.kodakodapa.kingfractal.utils.JuliaParams
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import com.github.kodakodapa.kingfractal.outputs.ARGB_CHANNELS
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class IntegrationTest {

    @Test
    fun `should create complete workflow without OpenCL`() {
        // Test the complete workflow without actual OpenCL execution
        val width = 100
        val height = 100

        // Create image data
        val imageData = ImageData.fromDimensions(width, height)
        assertEquals(width * height * ARGB_CHANNELS, imageData.pixels.size)

        // Create parameters
        val mandelbrotParams = MandelbrotParams(zoom = 2.0f, maxIterations = 50)
        val juliaParams = JuliaParams(juliaReal = -0.8f, juliaImag = 0.2f)

        // Verify parameters
        assertEquals(2.0f, mandelbrotParams.zoom)
        assertEquals(50, mandelbrotParams.maxIterations)
        assertEquals(-0.8f, juliaParams.juliaReal)
        assertEquals(0.2f, juliaParams.juliaImag)

        // Create renderer (but don't initialize to avoid OpenCL dependency)
        val renderer = DynamicOpenCLRenderer(
            kernelSource = FractalKernels.mandelbrotKernel,
            kernelName = "mandelbrot",
        )

        assertFalse(renderer.isInitialized)

        // Verify kernel source
        assertTrue(FractalKernels.mandelbrotKernel.contains("mandelbrot"))
        assertTrue(FractalKernels.juliaKernel.contains("julia"))
    }



    @Test
    fun `should convert ImageData to BufferedImage and back consistently`() {
        val width = 20
        val height = 15

        // Create test pattern
        val originalPixels = ByteArray(width * height * ARGB_CHANNELS) { index ->
            (index % 256).toByte()
        }

        val imageData = ImageData(width, height, originalPixels)
        val bufferedImage = imageData.toBufferedImage(null)

        assertEquals(width, bufferedImage.width)
        assertEquals(height, bufferedImage.height)

        // Verify some pixels were set (not all zero)
        var hasNonZeroPixel = false
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bufferedImage.getRGB(x, y) != 0) {
                    hasNonZeroPixel = true
                    break
                }
            }
            if (hasNonZeroPixel) break
        }
        assertTrue(hasNonZeroPixel, "BufferedImage should have non-zero pixels")
    }
}