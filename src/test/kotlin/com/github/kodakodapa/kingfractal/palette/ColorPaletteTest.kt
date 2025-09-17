package com.github.kodakodapa.kingfractal.palette

import kotlin.test.*

class ColorPaletteTest {
    
    @Test
    fun testGradientPalette() {
        val palette = GradientPalette("Test", 0xFF0000, 0x00FF00) // Red to Green
        
        // Test extreme values
        assertEquals(0x000000, palette.getColor(100, 100)) // Should be black for max iterations
        assertEquals(0xFF0000, palette.getColor(0, 100)) // Should be red for 0 iterations
        
        // Test that the color approaches green for high iteration values
        val nearMaxColor = palette.getColor(99, 100)
        val greenComponent = (nearMaxColor shr 8) and 0xFF
        val redComponent = (nearMaxColor shr 16) and 0xFF
        assertTrue(greenComponent > redComponent, "Near max iterations should be more green than red")
        
        // Test middle value - should have both red and green components
        val midColor = palette.getColor(50, 100)
        val midRed = (midColor shr 16) and 0xFF
        val midGreen = (midColor shr 8) and 0xFF
        assertTrue(midRed > 0, "Mid value should have red component")
        assertTrue(midGreen > 0, "Mid value should have green component")
    }
    
    @Test
    fun testRainbowPalette() {
        val palette = RainbowPalette()
        
        assertEquals("Rainbow", palette.name)
        
        // Test extreme values
        assertEquals(0x000000, palette.getColor(100, 100)) // Should be black for max iterations
        
        // Test that different iteration values produce different colors
        val color1 = palette.getColor(25, 100)
        val color2 = palette.getColor(50, 100)
        val color3 = palette.getColor(75, 100)
        
        assertNotEquals(color1, color2)
        assertNotEquals(color2, color3)
        assertNotEquals(color1, color3)
        
        // All colors should be non-black for non-max iterations
        assertNotEquals(0x000000, color1)
        assertNotEquals(0x000000, color2)
        assertNotEquals(0x000000, color3)
    }
    
    @Test
    fun testFirePalette() {
        val palette = FirePalette()
        
        assertEquals("Fire", palette.name)
        assertEquals(0x000000, palette.getColor(100, 100)) // Should be black for max iterations
        
        // Test progression from black/red to yellow
        val lowColor = palette.getColor(10, 100) // Should be reddish
        val midColor = palette.getColor(50, 100) // Should be orange
        val highColor = palette.getColor(90, 100) // Should be yellowish
        
        // Low iterations should have primarily red component
        assertTrue((lowColor shr 16) and 0xFF > 0)
        
        // High iterations should have red and green components (yellow)
        assertTrue((highColor shr 16) and 0xFF > 0)
        assertTrue((highColor shr 8) and 0xFF > 0)
    }
    
    @Test
    fun testCoolBluePalette() {
        val palette = CoolBluePalette()
        
        assertEquals("Cool Blue", palette.name)
        assertEquals(0x000000, palette.getColor(100, 100)) // Should be black for max iterations
        
        val color = palette.getColor(50, 100)
        
        // Should have blue component
        assertTrue((color and 0xFF) > 0)
        
        // Red component should be minimal or zero
        assertEquals(0, (color shr 16) and 0xFF)
    }
}

class PaletteRegistryTest {
    
    @Test
    fun testPaletteRegistration() {
        val testPalette = GradientPalette("Test Palette", 0xFF0000, 0x00FF00)
        PaletteRegistry.register(testPalette)
        
        val retrieved = PaletteRegistry.getPalette("Test Palette")
        assertNotNull(retrieved)
        assertEquals("Test Palette", retrieved.name)
    }
    
    @Test
    fun testGetNonExistentPalette() {
        val result = PaletteRegistry.getPalette("Non-Existent Palette")
        assertNull(result)
    }
    
    @Test
    fun testDefaultPalettesAvailable() {
        val paletteNames = PaletteRegistry.getPaletteNames()
        
        assertTrue(paletteNames.contains("Rainbow"))
        assertTrue(paletteNames.contains("Fire"))
        assertTrue(paletteNames.contains("Cool Blue"))
        assertTrue(paletteNames.contains("Red-Yellow"))
        assertTrue(paletteNames.contains("Purple-Pink"))
        assertTrue(paletteNames.contains("Green-Cyan"))
    }
    
    @Test
    fun testGetAllPalettes() {
        val allPalettes = PaletteRegistry.getAllPalettes()
        
        assertTrue(allPalettes.size >= 6) // At least the default palettes
        assertTrue(allPalettes.any { it.name == "Rainbow" })
        assertTrue(allPalettes.any { it.name == "Fire" })
        assertTrue(allPalettes.any { it.name == "Cool Blue" })
    }
}