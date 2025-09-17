package com.github.kodakodapa.kingfractal

import com.github.kodakodapa.kingfractal.OpenCLRenderer
import com.github.kodakodapa.kingfractal.outputs.ImageData
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull


class OpenCLRendererTest {
    private lateinit var renderer: OpenCLRenderer<ImageData>
    private val testKernelSource = """
        __kernel void test_kernel(__global unsigned char* output, int width, int height) {
            int x = get_global_id(0);
            int y = get_global_id(1);
            if (x < width && y < height) {
                int idx = (y * width + x) * 3;
                output[idx] = 255;     // R
                output[idx + 1] = 0;   // G  
                output[idx + 2] = 0;   // B
            }
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
        renderer = OpenCLRenderer(
            kernelSource = testKernelSource,
            kernelName = "test_kernel",
            dataFactory = { bytes -> ImageData.fromByteArray(10, 10, bytes) }
        )
    }

    @AfterEach
    fun tearDown() {
        if (renderer.isInitialized) {
            renderer.cleanup()
        }
    }

    @Test
    fun `should not be initialized initially`() {
        assertFalse(renderer.isInitialized)
    }

    @Test
    fun `should throw exception when executing without initialization`() {
        val imageData = ImageData.fromDimensions(10, 10)
        val params = MandelbrotParams()

        assertThrows<IllegalArgumentException> {
            renderer.execute(imageData, 10, 10, params)
        }

    }


    @Test
    fun `should create renderer with correct parameters`() {
        val kernelSource = "test kernel source"
        val kernelName = "test_kernel"
        val dataFactory: (ByteArray) -> ImageData = { bytes ->
            ImageData.fromByteArray(5, 5, bytes)
        }

        val testRenderer = OpenCLRenderer(kernelSource, kernelName, dataFactory)

        assertNotNull(testRenderer)
        assertFalse(testRenderer.isInitialized)
    }
}
