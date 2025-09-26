package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.outputs.ARGB_CHANNELS
import com.github.kodakodapa.kingfractal.twodimensional.DynamicOpenCLRenderer
import com.github.kodakodapa.kingfractal.twodimensional.kernels.FractalKernels
import com.github.kodakodapa.kingfractal.utils.JuliaParams
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assumptions.assumeTrue

class DynamicOpenCLRendererTest {

    private lateinit var mandelbrotRenderer: DynamicOpenCLRenderer
    private lateinit var juliaRenderer: DynamicOpenCLRenderer

    @BeforeEach
    fun setUp() {
        mandelbrotRenderer = DynamicOpenCLRenderer(
            kernelSource = FractalKernels.mandelbrotKernel,
            kernelName = "mandelbrot"
        )
        juliaRenderer = DynamicOpenCLRenderer(
            kernelSource = FractalKernels.juliaKernel,
            kernelName = "julia"
        )
    }

    @AfterEach
    fun tearDown() {
        try {
            mandelbrotRenderer.cleanup()
            juliaRenderer.cleanup()
        } catch (e: Exception) {
            // Ignore cleanup errors in tests
        }
    }

    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {

        @Test
        fun `should start with uninitialized state`() {
            assertFalse(mandelbrotRenderer.isInitialized)
            assertFalse(juliaRenderer.isInitialized)
        }

        @Test
        fun `should initialize successfully when OpenCL is available`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")

            assertDoesNotThrow {
                mandelbrotRenderer.initialize()
            }
            assertTrue(mandelbrotRenderer.isInitialized)
        }

        @Test
        fun `should handle OpenCL unavailable gracefully`() {
            if (!isOpenCLAvailable()) {
                assertThrows<RuntimeException> {
                    mandelbrotRenderer.initialize()
                }
                assertFalse(mandelbrotRenderer.isInitialized)
            }
        }

        @Test
        fun `should initialize multiple renderers independently`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")

            assertDoesNotThrow {
                mandelbrotRenderer.initialize()
                juliaRenderer.initialize()
            }
            assertTrue(mandelbrotRenderer.isInitialized)
            assertTrue(juliaRenderer.isInitialized)
        }
    }

    @Nested
    @DisplayName("Rendering Tests")
    inner class RenderingTests {

        @Test
        fun `should throw exception when rendering without initialization`() {
            val params = MandelbrotParams(1.0f, 0.0f, 0.0f, 100)

            val exception = assertThrows<IllegalArgumentException> {
                mandelbrotRenderer.renderFractal(800, 600, params)
            }
            assertEquals("Must be initialized", exception.message)
        }

        @Test
        fun `should render Mandelbrot fractal with correct dimensions`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 100)
            val width = 64
            val height = 48

            val result = mandelbrotRenderer.renderFractal(width, height, params)

            assertEquals(width, result.width)
            assertEquals(height, result.height)
            assertEquals(width * height * ARGB_CHANNELS, result.pixels.size)
        }

        @Test
        fun `should render Julia fractal with correct dimensions`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            juliaRenderer.initialize()
            val params = JuliaParams(1.0f, 0.0f, 0.0f, -0.7f, 0.27015f, 100)
            val width = 32
            val height = 32

            val result = juliaRenderer.renderFractal(width, height, params)

            assertEquals(width, result.width)
            assertEquals(height, result.height)
            assertEquals(width * height * ARGB_CHANNELS, result.pixels.size)
        }

        @Test
        fun `should handle different image dimensions dynamically`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 50)

            // Test various dimensions
            val dimensions = listOf(
                Pair(100, 100),
                Pair(200, 150),
                Pair(50, 75),
                Pair(1024, 768)
            )

            dimensions.forEach { (width, height) ->
                val result = mandelbrotRenderer.renderFractal(width, height, params)
                assertEquals(width, result.width, "Width should match for ${width}x${height}")
                assertEquals(height, result.height, "Height should match for ${width}x${height}")
                assertEquals(width * height * ARGB_CHANNELS, result.pixels.size, "Pixel array size should match for ${width}x${height}")
            }
        }

        @Test
        fun `should handle edge case dimensions`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 50)

            // Test minimum dimensions
            val result1 = mandelbrotRenderer.renderFractal(1, 1, params)
            assertEquals(1, result1.width)
            assertEquals(1, result1.height)

            // Test small dimensions
            val result2 = mandelbrotRenderer.renderFractal(2, 3, params)
            assertEquals(2, result2.width)
            assertEquals(3, result2.height)
        }

        @Test
        fun `should produce different results for different parameters`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val width = 64
            val height = 64

            val params1 = MandelbrotParams(1.0f, -0.5f, 0.0f, 100)
            val params2 = MandelbrotParams(2.0f, -0.5f, 0.0f, 100)

            val result1 = mandelbrotRenderer.renderFractal(width, height, params1)
            val result2 = mandelbrotRenderer.renderFractal(width, height, params2)

            // Results should be different for different zoom levels
            assertFalse(result1.pixels.contentEquals(result2.pixels))
        }

        @Test
        fun `should produce consistent results for same parameters`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 100)
            val width = 32
            val height = 32

            val result1 = mandelbrotRenderer.renderFractal(width, height, params)
            val result2 = mandelbrotRenderer.renderFractal(width, height, params)

            // Results should be identical for same parameters
            assertArrayEquals(result1.pixels, result2.pixels)
        }
    }

    @Nested
    @DisplayName("Parameter Validation Tests")
    inner class ParameterValidationTests {

        @Test
        fun `should handle extreme zoom values`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()

            // Very small zoom
            val params1 = MandelbrotParams(0.001f, 0.0f, 0.0f, 50)
            assertDoesNotThrow {
                mandelbrotRenderer.renderFractal(32, 32, params1)
            }

            // Very large zoom
            val params2 = MandelbrotParams(1000.0f, 0.0f, 0.0f, 50)
            assertDoesNotThrow {
                mandelbrotRenderer.renderFractal(32, 32, params2)
            }
        }

        @Test
        fun `should handle extreme center coordinates`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()

            val params = MandelbrotParams(1.0f, -10.0f, 10.0f, 50)
            assertDoesNotThrow {
                mandelbrotRenderer.renderFractal(32, 32, params)
            }
        }

        @Test
        fun `should handle different iteration counts`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()

            // Low iterations
            val params1 = MandelbrotParams(1.0f, -0.5f, 0.0f, 10)
            val result1 = mandelbrotRenderer.renderFractal(32, 32, params1)
            assertNotNull(result1)

            // High iterations
            val params2 = MandelbrotParams(1.0f, -0.5f, 0.0f, 1000)
            val result2 = mandelbrotRenderer.renderFractal(32, 32, params2)
            assertNotNull(result2)
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    inner class CleanupTests {

        @Test
        fun `should handle cleanup before initialization`() {
            assertDoesNotThrow {
                mandelbrotRenderer.cleanup()
            }
        }

        @Test
        fun `should handle cleanup after initialization`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            assertTrue(mandelbrotRenderer.isInitialized)

            assertDoesNotThrow {
                mandelbrotRenderer.cleanup()
            }
            assertFalse(mandelbrotRenderer.isInitialized)
        }

        @Test
        fun `should handle multiple cleanup calls`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()

            assertDoesNotThrow {
                mandelbrotRenderer.cleanup()
                mandelbrotRenderer.cleanup() // Second cleanup should not throw
            }
        }

        @Test
        fun `should not allow rendering after cleanup`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            mandelbrotRenderer.cleanup()

            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 100)
            assertThrows<IllegalArgumentException> {
                mandelbrotRenderer.renderFractal(32, 32, params)
            }
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        fun `should provide meaningful error for invalid kernel source`() {
            val invalidRenderer = DynamicOpenCLRenderer(
                kernelSource = "invalid kernel source",
                kernelName = "invalid"
            )

            if (isOpenCLAvailable()) {
                assertThrows<RuntimeException> {
                    invalidRenderer.initialize()
                }
            }
        }

        @Test
        fun `should provide meaningful error for invalid kernel name`() {
            val invalidRenderer = DynamicOpenCLRenderer(
                kernelSource = FractalKernels.mandelbrotKernel,
                kernelName = "nonexistent_kernel"
            )

            if (isOpenCLAvailable()) {
                assertThrows<RuntimeException> {
                    invalidRenderer.initialize()
                }
            }
        }

        @Test
        fun `should handle zero dimensions gracefully`() {
            assumeTrue(isOpenCLAvailable(), "OpenCL not available, skipping test")
            mandelbrotRenderer.initialize()
            val params = MandelbrotParams(1.0f, -0.5f, 0.0f, 100)

            // Zero width should be handled gracefully or throw meaningful exception
            assertThrows<Exception> {
                mandelbrotRenderer.renderFractal(0, 100, params)
            }

            // Zero height should be handled gracefully or throw meaningful exception
            assertThrows<Exception> {
                mandelbrotRenderer.renderFractal(100, 0, params)
            }
        }
    }

    companion object {
        @JvmStatic
        fun isOpenCLAvailable(): Boolean {
            return try {
                Class.forName("org.jocl.CL")
                val numPlatformsArray = IntArray(1)
                org.jocl.CL.clGetPlatformIDs(0, null, numPlatformsArray)
                numPlatformsArray[0] > 0
            } catch (e: Exception) {
                false
            }
        }
    }
}