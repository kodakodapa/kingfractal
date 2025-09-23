package com.github.kodakodapa.kingfractal.colors

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.*

class ARGBDistinctMandelbrotPaletteTest {

    @Test
    fun `test palette has correct name`() {
        val palette = ARGBDistinctMandelbrotPalette()
        assertEquals("Distinct Mandelbrot (1000 Colors)", palette.name)
    }

    @Test
    fun `test palette does not support transparency`() {
        val palette = ARGBDistinctMandelbrotPalette()
        assertFalse(palette.supportsTransparency)
    }

    @Test
    fun `test edge cases return black`() {
        val palette = ARGBDistinctMandelbrotPalette()

        assertEquals(ARGBColor.BLACK, palette.getColor(-1, 100))
        assertEquals(ARGBColor.BLACK, palette.getColor(100, 100))
        assertEquals(ARGBColor.BLACK, palette.getColor(0, 0))
        assertEquals(ARGBColor.BLACK, palette.getColor(0, -1))
    }

    @Test
    fun `test all colors have full alpha`() {
        val palette = ARGBDistinctMandelbrotPalette()

        for (i in 0 until 100) {
            val color = palette.getColor(i, 100)
            assertEquals(255, color.alpha, "Color at iteration $i should have full alpha")
        }
    }

    @Test
    fun `test consecutive colors are distinct`() {
        val palette = ARGBDistinctMandelbrotPalette()
        val maxIterations = 100

        var totalDistance = 0.0
        var minDistance = Double.MAX_VALUE

        for (i in 0 until maxIterations - 1) {
            val color1 = palette.getColor(i, maxIterations)
            val color2 = palette.getColor(i + 1, maxIterations)

            // Calculate color distance
            val distance = colorDistance(color1, color2)
            totalDistance += distance
            minDistance = min(minDistance, distance)

            // Assert colors are not identical
            assertNotEquals(color1, color2, "Consecutive colors at iterations $i and ${i+1} should be different")
        }

        val avgDistance = totalDistance / (maxIterations - 1)

        // Ensure minimum distance is reasonable (colors are visually distinct)
        assertTrue(minDistance > 20.0, "Minimum distance between consecutive colors ($minDistance) should be > 20")
        assertTrue(avgDistance > 50.0, "Average distance between consecutive colors ($avgDistance) should be > 50")

        println("Minimum consecutive color distance: $minDistance")
        println("Average consecutive color distance: $avgDistance")
    }

    @Test
    fun `test color distribution covers RGB space well`() {
        val palette = ARGBDistinctMandelbrotPalette()
        val maxIterations = 1000

        var minR = 255
        var maxR = 0
        var minG = 255
        var maxG = 0
        var minB = 255
        var maxB = 0

        for (i in 0 until maxIterations) {
            val color = palette.getColor(i, maxIterations)
            minR = min(minR, color.red)
            maxR = max(maxR, color.red)
            minG = min(minG, color.green)
            maxG = max(maxG, color.green)
            minB = min(minB, color.blue)
            maxB = max(maxB, color.blue)
        }

        // Check that we use a good range of the color space
        assertTrue(maxR - minR > 200, "Red range should be substantial")
        assertTrue(maxG - minG > 200, "Green range should be substantial")
        assertTrue(maxB - minB > 200, "Blue range should be substantial")

        println("RGB ranges: R[$minR-$maxR], G[$minG-$maxG], B[$minB-$maxB]")
    }

    @Test
    fun `test get color by index works correctly`() {
        val palette = ARGBDistinctMandelbrotPalette()

        // Test direct index access
        val color0 = palette.getColorByIndex(0)
        val color1 = palette.getColorByIndex(1)
        val color999 = palette.getColorByIndex(999)

        assertNotNull(color0)
        assertNotNull(color1)
        assertNotNull(color999)

        // Colors should be different
        assertNotEquals(color0, color1)
        assertNotEquals(color1, color999)

        // Test wraparound
        val color1000 = palette.getColorByIndex(1000)
        assertEquals(color0, color1000, "Index 1000 should wrap to index 0")
    }

    @Test
    fun `test palette size is correct`() {
        val palette = ARGBDistinctMandelbrotPalette()
        assertEquals(1000, palette.getPaletteSize())
    }

    private fun colorDistance(color1: ARGBColor, color2: ARGBColor): Double {
        val dr = (color1.red - color2.red) * 0.3
        val dg = (color1.green - color2.green) * 0.59
        val db = (color1.blue - color2.blue) * 0.11
        return sqrt(dr * dr + dg * dg + db * db)
    }
}