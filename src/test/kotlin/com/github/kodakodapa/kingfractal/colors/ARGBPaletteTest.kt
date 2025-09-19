package com.github.kodakodapa.kingfractal.colors

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import kotlin.math.abs

class ARGBPaletteTest {

    @Test
    fun `test rainbow palette basic functionality`() {
        val palette = ARGBRainbowPalette(enableTransparency = false)

        assertEquals("Rainbow", palette.name)
        assertFalse(palette.supportsTransparency)

        // Test edge cases
        val maxColor = palette.getColor(255, 255)
        assertEquals(ARGBColor.BLACK, maxColor)

        // Test that colors change across the spectrum
        val color1 = palette.getColor(0, 255)
        val color2 = palette.getColor(127, 255)
        val color3 = palette.getColor(254, 255)

        assertNotEquals(color1, color2)
        assertNotEquals(color2, color3)
        assertNotEquals(color1, color3)

        // All colors should be fully opaque
        assertEquals(255, color1.alpha)
        assertEquals(255, color2.alpha)
        assertEquals(255, color3.alpha)
    }

    @Test
    fun `test rainbow palette with transparency`() {
        val palette = ARGBRainbowPalette(enableTransparency = true, minAlpha = 100)

        assertEquals("Rainbow (Transparent)", palette.name)
        assertTrue(palette.supportsTransparency)

        // Test alpha values change
        val color1 = palette.getColor(0, 255)
        val color2 = palette.getColor(127, 255)
        val color3 = palette.getColor(254, 255)

        // Alpha should increase with iterations
        assertTrue(color1.alpha < color2.alpha)
        assertTrue(color2.alpha < color3.alpha)

        // All alphas should be at least minAlpha
        assertTrue(color1.alpha >= 100)
        assertTrue(color2.alpha >= 100)
        assertTrue(color3.alpha >= 100)
    }

    @Test
    fun `test fire palette basic functionality`() {
        val palette = ARGBFirePalette(enableSmoke = false)

        assertEquals("Fire", palette.name)
        assertFalse(palette.supportsTransparency)

        // Test color progression from black to red to orange to yellow
        val blackish = palette.getColor(0, 255)
        val reddish = palette.getColor(84, 255)  // ~1/3
        val orangish = palette.getColor(168, 255) // ~2/3
        val yellowish = palette.getColor(254, 255)

        // Red component should increase
        assertTrue(blackish.red < reddish.red)
        assertTrue(reddish.red <= orangish.red)

        // Green should be low in red phase, higher in orange/yellow
        assertTrue(reddish.green < orangish.green)
        assertTrue(orangish.green <= yellowish.green)

        // Blue should stay low throughout fire palette
        assertTrue(reddish.blue <= 10)
        assertTrue(orangish.blue <= 10)
    }

    @Test
    fun `test fire palette with smoke`() {
        val palette = ARGBFirePalette(enableSmoke = true)

        assertEquals("Fire (with Smoke)", palette.name)
        assertTrue(palette.supportsTransparency)

        // Test that smoke affects alpha
        val earlyColor = palette.getColor(0, 255)
        val midColor = palette.getColor(127, 255)
        val lateColor = palette.getColor(254, 255)

        // Early colors should have lower alpha (more smoke)
        assertTrue(earlyColor.alpha < 255)
    }

    @Test
    fun `test cool blue palette`() {
        val palette = ARGBCoolBluePalette(enableIceEffect = false)

        assertEquals("Cool Blue", palette.name)
        assertFalse(palette.supportsTransparency)

        val color1 = palette.getColor(0, 255)
        val color2 = palette.getColor(127, 255)
        val color3 = palette.getColor(254, 255)

        // Blue component should increase
        assertTrue(color1.blue < color2.blue)
        assertTrue(color2.blue < color3.blue)

        // Red should stay at 0
        assertEquals(0, color1.red)
        assertEquals(0, color2.red)
        assertEquals(0, color3.red)

        // Green should increase but less than blue
        assertTrue(color1.green <= color2.green)
        assertTrue(color2.green <= color3.green)
        assertTrue(color3.green <= color3.blue)
    }

    @Test
    fun `test cool blue palette with ice effect`() {
        val palette = ARGBCoolBluePalette(enableIceEffect = true)

        assertEquals("Cool Blue (Ice)", palette.name)
        assertTrue(palette.supportsTransparency)

        val color1 = palette.getColor(0, 255)
        val color2 = palette.getColor(254, 255)

        // Alpha should increase with iterations for ice effect
        assertTrue(color1.alpha < color2.alpha)
        assertTrue(color1.alpha >= 128) // Should be at least semi-transparent
    }

    @Test
    fun `test plasma palette oscillations`() {
        val palette = ARGBPlasmaPalette(enableEnergyEffect = false)

        assertEquals("Plasma", palette.name)
        assertFalse(palette.supportsTransparency)

        // Sample several points to verify oscillating behavior
        val samples = (0..254 step 32).map { i ->
            palette.getColor(i, 255)
        }

        // Verify we get variety in colors (plasma should oscillate)
        val redValues = samples.map { it.red }.toSet()
        val greenValues = samples.map { it.green }.toSet()
        val blueValues = samples.map { it.blue }.toSet()

        // Should have multiple different values due to oscillation
        assertTrue(redValues.size > 3)
        assertTrue(greenValues.size > 3)
        assertTrue(blueValues.size > 3)
    }

    @Test
    fun `test plasma palette with energy effect`() {
        val palette = ARGBPlasmaPalette(enableEnergyEffect = true)

        assertEquals("Plasma (Energy)", palette.name)
        assertTrue(palette.supportsTransparency)

        // Energy effect should cause alpha to oscillate
        val samples = (0..254 step 16).map { i ->
            palette.getColor(i, 255)
        }

        val alphaValues = samples.map { it.alpha }.toSet()

        // Should have multiple alpha values due to energy pulsing
        assertTrue(alphaValues.size > 3)

        // All alpha values should be within expected range
        alphaValues.forEach { alpha ->
            assertTrue(alpha in 55..255)
        }
    }

    @Test
    fun `test gradient palette linear interpolation`() {
        val startColor = ARGBColor(255, 255, 0, 0) // Red
        val endColor = ARGBColor(255, 0, 0, 255)   // Blue
        val palette = ARGBGradientPalette("Test Gradient", startColor, endColor)

        assertEquals("Test Gradient", palette.name)
        assertEquals(startColor.alpha < 255 || endColor.alpha < 255, palette.supportsTransparency)

        // Test start and end points
        val color0 = palette.getColor(0, 255)
        val color255 = palette.getColor(255, 255)

        assertEquals(startColor, color0)
        assertEquals(ARGBColor.BLACK, color255) // Points in set should be black

        // Test midpoint
        val colorMid = palette.getColor(127, 255)

        // Should be roughly halfway between red and blue
        assertTrue(colorMid.red > 0 && colorMid.red < 255)
        assertTrue(colorMid.blue > 0 && colorMid.blue < 255)
        assertTrue(abs(colorMid.red - colorMid.blue) < 50) // Should be roughly equal
    }

    @Test
    fun `test gradient palette with transparency`() {
        val startColor = ARGBColor(255, 255, 0, 0)
        val endColor = ARGBColor(100, 0, 255, 0)
        val palette = ARGBGradientPalette("Transparent Gradient", startColor, endColor)

        assertTrue(palette.supportsTransparency)

        val color0 = palette.getColor(0, 255)
        val colorMid = palette.getColor(127, 255)

        assertEquals(255, color0.alpha)
        assertTrue(colorMid.alpha > 100 && colorMid.alpha < 255)
    }

    @Test
    fun `test layered palette composition`() {
        val palette1 = ARGBRainbowPalette(false)
        val palette2 = ARGBFirePalette(false)

        val layers = listOf(
            palette1 to 0.7f,
            palette2 to 0.3f
        )

        val layeredPalette = ARGBLayeredPalette("Test Layered", layers)

        assertEquals("Test Layered", layeredPalette.name)
        assertTrue(layeredPalette.supportsTransparency)

        // Test that it produces different colors than individual palettes
        val layeredColor = layeredPalette.getColor(127, 255)
        val rainbow127 = palette1.getColor(127, 255)
        val fire127 = palette2.getColor(127, 255)

        assertNotEquals(layeredColor, rainbow127)
        assertNotEquals(layeredColor, fire127)

        // Alpha should be a blend of the input alphas weighted by opacity
        assertTrue(layeredColor.alpha > 0)
    }

    @Test
    fun `test color matrix generation`() {
        val palette = ARGBRainbowPalette(false)
        val matrix = palette.generateColorMatrix(255)

        assertEquals(255, matrix.size)

        // Test that matrix colors match getColor results
        for (i in 0..254) {
            val directColor = palette.getColor(i, 255)
            val matrixColor = matrix[i]
            assertEquals(directColor, matrixColor)
        }
    }

    @Test
    fun `test color vector conversion`() {
        val palette = ARGBRainbowPalette(false)
        val color = palette.getColor(100, 255)
        val vector = palette.getColorVector(100, 255)

        assertEquals(4, vector.size)
        assertEquals(color.alpha, vector[0])
        assertEquals(color.red, vector[1])
        assertEquals(color.green, vector[2])
        assertEquals(color.blue, vector[3])
    }

    @Test
    fun `test all palettes handle edge cases correctly`() {
        val palettes = listOf(
            ARGBRainbowPalette(),
            ARGBFirePalette(),
            ARGBCoolBluePalette(),
            ARGBPlasmaPalette(),
            ARGBGradientPalette("Test", ARGBColor.RED, ARGBColor.BLUE)
        )

        for (palette in palettes) {
            // Test iteration at maximum should return BLACK
            assertEquals(ARGBColor.BLACK, palette.getColor(255, 255))
            assertEquals(ARGBColor.BLACK, palette.getColor(100, 100))

            // Test beyond maximum should return BLACK
            assertEquals(ARGBColor.BLACK, palette.getColor(300, 255))

            // Test valid colors are in range
            val color = palette.getColor(50, 255)
            assertTrue(color.alpha in 0..255)
            assertTrue(color.red in 0..255)
            assertTrue(color.green in 0..255)
            assertTrue(color.blue in 0..255)
        }
    }
}