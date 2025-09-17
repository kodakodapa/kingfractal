package com.github.kodakodapa.kingfractal.palette

import com.github.kodakodapa.kingfractal.color.ARGBColor
import kotlin.test.*

class ARGBPaletteTest {
    
    @Test
    fun testARGBGradientPalette() {
        val startColor = ARGBColor(255, 255, 0, 0)  // Red
        val endColor = ARGBColor(128, 0, 255, 0)    // Semi-transparent green
        val palette = ARGBGradientPalette("Test Gradient", startColor, endColor)
        
        assertEquals("Test Gradient", palette.name)
        assertTrue(palette.supportsTransparency) // Because end color has alpha < 255
        
        // Test extreme values
        assertEquals(ARGBColor.BLACK, palette.getColor(100, 100)) // Max iterations = black
        assertEquals(startColor, palette.getColor(0, 100))        // Zero iterations = start color
        
        // Test progression
        val midColor = palette.getColor(50, 100)
        assertTrue(midColor.alpha < startColor.alpha)
        assertTrue(midColor.green > startColor.green)
    }
    
    @Test
    fun testARGBRainbowPalette() {
        val opaquePalette = ARGBRainbowPalette(false)
        val transparentPalette = ARGBRainbowPalette(true, 100)
        
        assertEquals("Rainbow", opaquePalette.name)
        assertEquals("Rainbow (Transparent)", transparentPalette.name)
        assertFalse(opaquePalette.supportsTransparency)
        assertTrue(transparentPalette.supportsTransparency)
        
        // Test that both produce different colors for same input
        val color1 = opaquePalette.getColor(25, 100)
        val color2 = transparentPalette.getColor(25, 100)
        
        assertEquals(255, color1.alpha) // Opaque
        assertTrue(color2.alpha < 255)  // Transparent
        
        // Colors should be different due to transparency
        assertNotEquals(color1, color2)
    }
    
    @Test
    fun testARGBFirePalette() {
        val firePalette = ARGBFirePalette(false)
        val smokePalette = ARGBFirePalette(true)
        
        assertEquals("Fire", firePalette.name)
        assertEquals("Fire (with Smoke)", smokePalette.name)
        assertFalse(firePalette.supportsTransparency)
        assertTrue(smokePalette.supportsTransparency)
        
        // Test color progression
        val lowColor = firePalette.getColor(10, 100)
        val midColor = firePalette.getColor(50, 100)
        val highColor = firePalette.getColor(90, 100)
        
        // Low iterations should be primarily red
        assertTrue(lowColor.red > lowColor.green)
        assertTrue(lowColor.red > lowColor.blue)
        
        // High iterations should have red and green (yellow-ish)
        assertTrue(highColor.red > 0)
        assertTrue(highColor.green > 0)
    }
    
    @Test
    fun testARGBLayeredPalette() {
        val basePalette = ARGBRainbowPalette(false)
        val overlayPalette = ARGBFirePalette(true)
        
        val layeredPalette = ARGBLayeredPalette(
            "Test Layered",
            listOf(
                basePalette to 0.7f,
                overlayPalette to 0.3f
            )
        )
        
        assertEquals("Test Layered", layeredPalette.name)
        assertTrue(layeredPalette.supportsTransparency)
        
        // Test that layered result is different from individual palettes
        val baseColor = basePalette.getColor(50, 100)
        val overlayColor = overlayPalette.getColor(50, 100)
        val layeredColor = layeredPalette.getColor(50, 100)
        
        assertNotEquals(baseColor, layeredColor)
        assertNotEquals(overlayColor, layeredColor)
    }
    
    @Test
    fun testColorMatrixGeneration() {
        val palette = ARGBRainbowPalette(false)
        val matrix = palette.generateColorMatrix(100)
        
        assertEquals(255, matrix.size) // Default matrix size
        
        // Test that colors are generated correctly
        val color0 = matrix[0]
        val color50 = matrix[50]
        val color100 = matrix[100]
        
        // Should be different colors
        assertNotEquals(color0, color50)
        assertNotEquals(color50, color100)
        
        // Should match palette colors
        assertEquals(palette.getColor(0, 100), color0)
        assertEquals(palette.getColor(50, 100), color50)
        assertEquals(palette.getColor(100, 100), color100)
    }
    
    @Test
    fun testColorVectorAccess() {
        val palette = ARGBGradientPalette(
            "Test",
            ARGBColor(200, 255, 0, 0),
            ARGBColor(100, 0, 255, 0)
        )
        
        val color = palette.getColor(50, 100)
        val vector = palette.getColorVector(50, 100)
        
        assertContentEquals(color.toVector(), vector)
        assertEquals(4, vector.size)
    }
}

class ARGBPaletteRegistryTest {
    
    @Test
    fun testDefaultPalettesAvailable() {
        val paletteNames = ARGBPaletteRegistry.getPaletteNames()
        
        assertTrue(paletteNames.contains("Rainbow"))
        assertTrue(paletteNames.contains("Rainbow (Transparent)"))
        assertTrue(paletteNames.contains("Fire"))
        assertTrue(paletteNames.contains("Fire (with Smoke)"))
        assertTrue(paletteNames.contains("Cool Blue"))
        assertTrue(paletteNames.contains("Cool Blue (Ice)"))
        assertTrue(paletteNames.contains("Plasma"))
        assertTrue(paletteNames.contains("Plasma (Energy)"))
    }
    
    @Test
    fun testPaletteRegistration() {
        val testPalette = ARGBGradientPalette(
            "Test Registry Palette",
            ARGBColor.RED,
            ARGBColor.BLUE
        )
        
        ARGBPaletteRegistry.register(testPalette)
        
        val retrieved = ARGBPaletteRegistry.getPalette("Test Registry Palette")
        assertNotNull(retrieved)
        assertEquals("Test Registry Palette", retrieved.name)
    }
    
    @Test
    fun testTransparencyFiltering() {
        val transparentPalettes = ARGBPaletteRegistry.getTransparentPalettes()
        val opaquePalettes = ARGBPaletteRegistry.getOpaquePalettes()
        
        assertTrue(transparentPalettes.isNotEmpty())
        assertTrue(opaquePalettes.isNotEmpty())
        
        // Check that transparent palettes actually support transparency
        transparentPalettes.forEach { palette ->
            assertTrue(palette.supportsTransparency, "Palette ${palette.name} should support transparency")
        }
        
        // Check that opaque palettes don't support transparency
        opaquePalettes.forEach { palette ->
            assertFalse(palette.supportsTransparency, "Palette ${palette.name} should not support transparency")
        }
    }
    
    @Test
    fun testGenerateAllColorMatrices() {
        val allMatrices = ARGBPaletteRegistry.generateAllColorMatrices(100)
        
        assertTrue(allMatrices.isNotEmpty())
        
        // Check that we have matrices for key palettes
        assertTrue(allMatrices.containsKey("Rainbow"))
        assertTrue(allMatrices.containsKey("Fire"))
        assertTrue(allMatrices.containsKey("Cool Blue"))
        
        // Check matrix properties
        allMatrices.values.forEach { matrix ->
            assertEquals(255, matrix.size)
        }
    }
    
    @Test
    fun testGetNonExistentPalette() {
        val result = ARGBPaletteRegistry.getPalette("NonExistent Palette")
        assertNull(result)
    }
    
    @Test
    fun testGetAllPalettes() {
        val allPalettes = ARGBPaletteRegistry.getAllPalettes()
        assertTrue(allPalettes.size >= 10) // Should have at least the built-in palettes
        
        // Should contain both transparent and opaque variants
        val transparentCount = allPalettes.count { it.supportsTransparency }
        val opaqueCount = allPalettes.count { !it.supportsTransparency }
        
        assertTrue(transparentCount > 0)
        assertTrue(opaqueCount > 0)
    }
}