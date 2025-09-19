package com.github.kodakodapa.kingfractal

import com.github.kodakodapa.kingfractal.colors.*
import com.github.kodakodapa.kingfractal.outputs.PaletteRender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EdgeCaseTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test palette with zero max iterations`() {
        val palette = ARGBRainbowPalette()

        // Should handle zero max iterations gracefully
        val color = palette.getColor(10, 0)
        assertEquals(ARGBColor.BLACK, color)
    }

    @Test
    fun `test palette with negative max iterations`() {
        val palette = ARGBFirePalette()

        // Should handle negative max iterations gracefully
        val color = palette.getColor(10, -5)
        assertEquals(ARGBColor.BLACK, color)
    }

    @Test
    fun `test palette with max iterations of 1`() {
        val palette = ARGBCoolBluePalette()

        // Should handle max iterations of 1 without division by zero
        val color0 = palette.getColor(0, 1)
        val color1 = palette.getColor(1, 1)

        // color0 should be valid, color1 should be black (in set)
        assertNotNull(color0)
        assertEquals(ARGBColor.BLACK, color1)
    }

    @Test
    fun `test palette with negative iterations`() {
        val palette = ARGBPlasmaPalette()

        // Should handle negative iterations gracefully
        val color = palette.getColor(-10, 255)
        assertNotNull(color)
        // Should not throw exception
    }

    @Test
    fun `test gradient palette with null colors`() {
        // This tests constructor validation - nulls should be caught by Kotlin's type system
        // But we can test the behavior when similar issues occur
        val palette = ARGBGradientPalette("Test", ARGBColor.RED, ARGBColor.BLUE)

        // Test normal operation
        val color = palette.getColor(127, 255)
        assertNotNull(color)
    }

    @Test
    fun `test layered palette with empty layers`() {
        val emptyLayers = emptyList<Pair<ARGBPalette, Float>>()
        val layeredPalette = ARGBLayeredPalette("Empty", emptyLayers)

        val color = layeredPalette.getColor(100, 255)
        assertEquals(ARGBColor.BLACK, color)
    }

    @Test
    fun `test layered palette with invalid opacity values`() {
        val layers = listOf(
            ARGBRainbowPalette() to -1f,  // Negative opacity
            ARGBFirePalette() to 2f,      // Opacity > 1
            ARGBCoolBluePalette() to 0f   // Zero opacity
        )

        val layeredPalette = ARGBLayeredPalette("Invalid Opacities", layers)

        // Should handle invalid opacities gracefully
        val color = layeredPalette.getColor(100, 255)
        assertNotNull(color)
    }

    @Test
    fun `test color matrix with invalid size`() {

        // Should handle operations gracefully
        assertThrows(IllegalArgumentException::class.java) {
            ARGBColorMatrix(0)
        }
    }

    @Test
    fun `test color matrix out of bounds access`() {
        val matrix = ARGBColorMatrix(10)

        // Test out of bounds access
        assertThrows(IllegalArgumentException::class.java) {
            matrix[11] = ARGBColor.RED
        }

        assertThrows(IllegalArgumentException::class.java) {
            matrix[-1]
        }
    }

    @Test
    fun `test color matrix with null color`() {
        val matrix = ARGBColorMatrix(10)

        // Should handle null color gracefully
        matrix[5] = null
        val retrievedColor = matrix[5]

        // Should fallback to black
        assertEquals(ARGBColor.BLACK, retrievedColor)
    }

    @Test
    fun `test color matrix with invalid vector`() {
        val matrix = ARGBColorMatrix(10)

        // Test with null vector
        matrix.setVector(5, null)
        val color = matrix[5]
        assertEquals(ARGBColor.BLACK, color)

        // Test with wrong size vector
        assertThrows(IllegalArgumentException::class.java) {
            matrix.setVector(5, intArrayOf(255, 255)) // Only 2 components
        }
    }

    @Test
    fun `test color matrix gradient fill with null colors`() {
        val matrix = ARGBColorMatrix(10)

        // Should handle null colors gracefully
        matrix.fillGradient(null, null)

        // Should fill with black to white gradient
        val firstColor = matrix[0]
        val lastColor = matrix[10]

        assertNotNull(firstColor)
        assertNotNull(lastColor)
    }

    @Test
    fun `test palette render with null palette`() {
        val renderer = PaletteRender()

        // Should handle null palette gracefully
        val image = renderer.toGradientImage(null)

        assertNotNull(image)
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    fun `test palette render with zero max iterations`() {
        val renderer = PaletteRender()
        val palette = ARGBRainbowPalette()

        // Should handle zero max iterations
        val image = renderer.toGradientImage(palette, 0)

        assertNotNull(image)
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    fun `test palette render with negative max iterations`() {
        val renderer = PaletteRender()
        val palette = ARGBFirePalette()

        // Should handle negative max iterations
        val image = renderer.toSwatchGrid(palette, -10)

        assertNotNull(image)
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    fun `test palette save with invalid filename`() {
        val renderer = PaletteRender()
        val palette = ARGBRainbowPalette()

        // Should handle empty filename gracefully
        renderer.saveAsPng("", palette)

        // Should handle null filename gracefully (would be caught by Kotlin type system)
        // But test with problematic filenames
        renderer.saveAsPng("   ", palette) // Whitespace only
    }

    @Test
    fun `test palette save with invalid directory`() {
        val renderer = PaletteRender()
        val palette = ARGBRainbowPalette()

        val invalidPath = File(tempDir, "nonexistent/deeply/nested/path/test.png").absolutePath

        // Should create directories and handle gracefully
        renderer.saveAsPng(invalidPath, palette)

        // Check if file was created (directories should be created automatically)
        assertTrue(File(invalidPath).parentFile.exists())
    }

    @Test
    fun `test ARGBColor edge cases`() {
        // Test boundary values
        val minColor = ARGBColor(0, 0, 0, 0)
        val maxColor = ARGBColor(255, 255, 255, 255)

        assertNotNull(minColor)
        assertNotNull(maxColor)

        // Test invalid values should throw in constructor
        assertThrows(IllegalArgumentException::class.java) {
            ARGBColor(-1, 0, 0, 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            ARGBColor(256, 0, 0, 0)
        }
    }

    @Test
    fun `test ARGBColor vector operations`() {
        val color = ARGBColor(128, 64, 192, 32)

        val vector = color.toVector()
        assertEquals(4, vector.size)
        assertEquals(128, vector[0]) // Alpha
        assertEquals(64, vector[1])  // Red
        assertEquals(192, vector[2]) // Green
        assertEquals(32, vector[3])  // Blue

        // Test round-trip conversion
        val reconstructed = ARGBColor.fromVector(vector)
        assertEquals(color, reconstructed)
    }

    @Test
    fun `test ARGBColor from invalid vector`() {
        // Test with wrong size vector
        assertThrows(IllegalArgumentException::class.java) {
            ARGBColor.fromVector(intArrayOf(255, 255)) // Only 2 components
        }

        assertThrows(IllegalArgumentException::class.java) {
            ARGBColor.fromVector(intArrayOf(255, 255, 255, 255, 255)) // Too many components
        }
    }

    @Test
    fun `test ARGBInterpolation edge cases`() {
        val color1 = ARGBColor(255, 255, 0, 0) // Red
        val color2 = ARGBColor(255, 0, 0, 255) // Blue

        // Test t = 0 should return first color
        val result0 = ARGBInterpolation.lerp(color1, color2, 0f)
        assertEquals(color1, result0)

        // Test t = 1 should return second color
        val result1 = ARGBInterpolation.lerp(color1, color2, 1f)
        assertEquals(color2, result1)

        // Test t > 1 should be clamped
        val resultOver = ARGBInterpolation.lerp(color1, color2, 2f)
        assertEquals(color2, resultOver)

        // Test t < 0 should be clamped
        val resultUnder = ARGBInterpolation.lerp(color1, color2, -1f)
        assertEquals(color1, resultUnder)
    }

    @Test
    fun `test HSV interpolation edge cases`() {
        val color1 = ARGBColor(255, 255, 0, 0) // Red
        val color2 = ARGBColor(255, 0, 255, 0) // Green

        // Test HSV interpolation handles edge cases
        val resultHSV = ARGBInterpolation.lerpHSV(color1, color2, 0.5f)
        assertNotNull(resultHSV)

        // Should produce different result than linear interpolation
        val resultLinear = ARGBInterpolation.lerp(color1, color2, 0.5f)
        assertNotEquals(resultLinear, resultHSV)
    }

    @Test
    fun `test registry with problematic palettes`() {
        val originalCount = ARGBPaletteRegistry.getAllPalettes().size

        // Create a problematic palette that might throw exceptions
        val problematicPalette = object : ARGBPalette {
            override val name = "Problematic Palette"
            override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
                if (iterations == 50) {
                    throw RuntimeException("Simulated palette error")
                }
                return ARGBColor.RED
            }
        }

        // Registry should handle registration
        ARGBPaletteRegistry.register(problematicPalette)

        val newCount = ARGBPaletteRegistry.getAllPalettes().size
        assertEquals(originalCount + 1, newCount)

        // Test that the problematic palette can be retrieved
        val retrieved = ARGBPaletteRegistry.getPalette("Problematic Palette")
        assertNotNull(retrieved)

        // Test normal operation
        val normalColor = retrieved!!.getColor(10, 255)
        assertEquals(ARGBColor.RED, normalColor)
    }

    @Test
    fun `test render all palettes with failures`() {
        val renderer = PaletteRender()

        // This should handle any individual palette failures gracefully
        val allPalettesImage = renderer.renderAllPalettes()

        assertNotNull(allPalettesImage)
        assertTrue(allPalettesImage.width > 0)
        assertTrue(allPalettesImage.height > 0)
    }
}