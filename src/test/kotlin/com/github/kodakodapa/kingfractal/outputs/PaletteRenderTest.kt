package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class PaletteRenderTest {

    private lateinit var renderer: PaletteRender
    private lateinit var testPalette: ARGBPalette

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        renderer = PaletteRender()
        testPalette = ARGBRainbowPalette(enableTransparency = true)
    }

    @Test
    fun `test gradient image dimensions`() {
        val image = renderer.toGradientImage(testPalette)

        assertEquals(PaletteRender.GRADIENT_WIDTH, image.width)
        assertEquals(PaletteRender.GRADIENT_HEIGHT * 3, image.height)
        assertEquals(BufferedImage.TYPE_INT_ARGB, image.type)
    }

    @Test
    fun `test gradient image contains three rows`() {
        val image = renderer.toGradientImage(testPalette, maxIterations = 100)

        // Check that we have distinct rows by sampling pixels
        val row1Y = PaletteRender.GRADIENT_HEIGHT / 2
        val row2Y = PaletteRender.GRADIENT_HEIGHT + PaletteRender.GRADIENT_HEIGHT / 2
        val row3Y = PaletteRender.GRADIENT_HEIGHT * 2 + PaletteRender.GRADIENT_HEIGHT / 2

        // Sample multiple points across the gradient
        val samplePoints = listOf(50, 150, 250, 350, 450)

        for (x in samplePoints) {
            val pixel1 = image.getRGB(x, row1Y)
            val pixel2 = image.getRGB(x, row2Y)
            val pixel3 = image.getRGB(x, row3Y)

            // Row 1 (RGB only) should be fully opaque
            val alpha1 = (pixel1 shr 24) and 0xFF
            assertEquals(255, alpha1, "Row 1 should be fully opaque")

            // Row 2 (alpha as grayscale) should also be fully opaque but showing gray values
            val alpha2 = (pixel2 shr 24) and 0xFF
            assertEquals(255, alpha2, "Row 2 should be fully opaque")

            // Check that row 2 is grayscale (R=G=B)
            val r2 = (pixel2 shr 16) and 0xFF
            val g2 = (pixel2 shr 8) and 0xFF
            val b2 = pixel2 and 0xFF
            assertEquals(r2, g2, "Row 2 should be grayscale")
            assertEquals(g2, b2, "Row 2 should be grayscale")
        }
    }

    @Test
    fun `test swatch grid dimensions`() {
        val image = renderer.toSwatchGrid(testPalette)

        val expectedWidth = 16 * PaletteRender.SWATCH_SIZE * 2
        val expectedHeight = 16 * PaletteRender.SWATCH_SIZE

        assertEquals(expectedWidth, image.width)
        assertEquals(expectedHeight, image.height)
        assertEquals(BufferedImage.TYPE_INT_ARGB, image.type)
    }

    @Test
    fun `test swatch grid contains correct number of swatches`() {
        val image = renderer.toSwatchGrid(testPalette)

        // Check dimensions based on 16x16 grid
        val swatchWidth = PaletteRender.SWATCH_SIZE * 2
        val swatchHeight = PaletteRender.SWATCH_SIZE

        val numCols = image.width / swatchWidth
        val numRows = image.height / swatchHeight

        assertEquals(16, numCols)
        assertEquals(16, numRows)
    }

    @Test
    fun `test comprehensive image contains both visualizations`() {
        val image = renderer.toComprehensiveImage(testPalette)

        // Should be at least as tall as gradient + swatch + padding
        val minHeight = PaletteRender.GRADIENT_HEIGHT * 3 + (16 * PaletteRender.SWATCH_SIZE) + 40
        assertTrue(image.height >= minHeight)

        // Should be at least as wide as the gradient or swatch grid
        val minWidth = kotlin.math.max(
            PaletteRender.GRADIENT_WIDTH,
            16 * PaletteRender.SWATCH_SIZE * 2
        )
        assertTrue(image.width >= minWidth)
    }

    @Test
    fun `test render all palettes creates non-empty image`() {
        val image = renderer.renderAllPalettes()

        // Should create a valid image
        assertNotNull(image)
        assertTrue(image.width > 1)
        assertTrue(image.height > 1)

        // Should be large enough to contain multiple palettes
        val numPalettes = ARGBPaletteRegistry.getAllPalettes().size
        assertTrue(numPalettes > 0)

        // Height should be proportional to number of palettes
        val minHeightPerPalette = 100 // Rough minimum
        assertTrue(image.height > numPalettes * minHeightPerPalette)
    }

    @Test
    fun `test save as PNG creates valid file`() {
        val filename = File(tempDir, "test_palette.png").absolutePath

        renderer.saveAsPng(filename, testPalette)

        val file = File(filename)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // Verify it's a valid PNG by reading it back
        val loadedImage = ImageIO.read(file)
        assertNotNull(loadedImage)
    }

    @Test
    fun `test save all palettes as PNG creates valid file`() {
        val filename = File(tempDir, "all_palettes.png").absolutePath

        renderer.saveAllPalettesAsPng(filename)

        val file = File(filename)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)

        // Verify it's a valid PNG
        val loadedImage = ImageIO.read(file)
        assertNotNull(loadedImage)
    }

    @Test
    fun `test different palette types produce different visualizations`() {
        val rainbowPalette = ARGBRainbowPalette(false)
        val firePalette = ARGBFirePalette(true)

        val rainbowImage = renderer.toGradientImage(rainbowPalette)
        val fireImage = renderer.toGradientImage(firePalette)

        // Sample some pixels to ensure they're different
        var differentPixels = 0
        for (x in 0 until rainbowImage.width step 10) {
            val rainbowPixel = rainbowImage.getRGB(x, 0)
            val firePixel = fireImage.getRGB(x, 0)
            if (rainbowPixel != firePixel) {
                differentPixels++
            }
        }

        // Most pixels should be different between rainbow and fire palettes
        assertTrue(differentPixels > rainbowImage.width / 10 * 0.8)
    }

    @Test
    fun `test transparency visualization in gradient`() {
        val transparentPalette = ARGBGradientPalette(
            "Test Transparent",
            ARGBColor(255, 255, 0, 0),
            ARGBColor(0, 0, 255, 0)
        )

        val image = renderer.toGradientImage(transparentPalette)

        // Check alpha row (row 2) shows gradient from opaque to transparent
        val alphaRowY = PaletteRender.GRADIENT_HEIGHT + PaletteRender.GRADIENT_HEIGHT / 2

        val startPixel = image.getRGB(10, alphaRowY)
        val endPixel = image.getRGB(image.width - 10, alphaRowY)

        // Extract grayscale values (which represent alpha)
        val startGray = startPixel and 0xFF
        val endGray = endPixel and 0xFF

        // Start should be more opaque (higher value) than end
        assertTrue(startGray > endGray)
    }
}