package com.github.kodakodapa.kingfractal.colors

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

/**
 * Tests specifically for registry isolation and reset functionality
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class ARGBPaletteRegistryIsolationTest {

    @BeforeEach
    fun setUp() {
        // Start with a clean slate
        ARGBPaletteRegistry.reset(includeBuiltIn = true)
    }

    @Test
    fun `test reset clears all palettes when includeBuiltIn is false`() {
        // First, add a custom palette
        val customPalette = ARGBGradientPalette("Custom Test", ARGBColor.RED, ARGBColor.BLUE)
        ARGBPaletteRegistry.register(customPalette)

        // Verify it exists
        assertNotNull(ARGBPaletteRegistry.getPalette("Custom Test"))
        assertTrue(ARGBPaletteRegistry.getAllPalettes().isNotEmpty())

        // Reset without built-in palettes
        ARGBPaletteRegistry.reset(includeBuiltIn = false)

        // Should be completely empty
        assertTrue(ARGBPaletteRegistry.getAllPalettes().isEmpty())
        assertNull(ARGBPaletteRegistry.getPalette("Custom Test"))
        assertNull(ARGBPaletteRegistry.getPalette("Rainbow"))
    }

    @Test
    fun `test reset restores built-in palettes when includeBuiltIn is true`() {
        // Add a custom palette
        val customPalette = ARGBGradientPalette("Custom Test 2", ARGBColor.RED, ARGBColor.BLUE)
        ARGBPaletteRegistry.register(customPalette)

        val countWithCustom = ARGBPaletteRegistry.getAllPalettes().size

        // Reset with built-in palettes
        ARGBPaletteRegistry.reset(includeBuiltIn = true)

        // Custom palette should be gone
        assertNull(ARGBPaletteRegistry.getPalette("Custom Test 2"))

        // Built-in palettes should exist
        assertNotNull(ARGBPaletteRegistry.getPalette("Rainbow"))
        assertNotNull(ARGBPaletteRegistry.getPalette("Fire"))
        assertNotNull(ARGBPaletteRegistry.getPalette("Cool Blue"))

        // Should have the standard set of built-in palettes
        val builtInCount = ARGBPaletteRegistry.getAllPalettes().size
        assertTrue(builtInCount > 0)
        assertTrue(builtInCount < countWithCustom)
    }

    @Test
    fun `test registry state isolation between test runs`() {
        // This test verifies that state doesn't leak between tests

        // Start with known state
        val initialCount = ARGBPaletteRegistry.getAllPalettes().size

        // Add a uniquely named palette
        val timestamp = System.currentTimeMillis()
        val uniquePalette = ARGBGradientPalette(
            "Isolation Test $timestamp",
            ARGBColor.GREEN,
            ARGBColor.BLUE
        )
        ARGBPaletteRegistry.register(uniquePalette)

        // Verify it was added
        assertEquals(initialCount + 1, ARGBPaletteRegistry.getAllPalettes().size)
        assertNotNull(ARGBPaletteRegistry.getPalette("Isolation Test $timestamp"))

        // Reset for next test
        ARGBPaletteRegistry.reset(includeBuiltIn = true)

        // Verify custom palette is gone
        assertNull(ARGBPaletteRegistry.getPalette("Isolation Test $timestamp"))
        assertEquals(initialCount, ARGBPaletteRegistry.getAllPalettes().size)
    }

    @Test
    fun `test multiple resets are idempotent`() {
        val initialPalettes = ARGBPaletteRegistry.getAllPalettes().toList()
        val initialCount = initialPalettes.size

        // Reset multiple times
        ARGBPaletteRegistry.reset(includeBuiltIn = true)
        val countAfterFirst = ARGBPaletteRegistry.getAllPalettes().size

        ARGBPaletteRegistry.reset(includeBuiltIn = true)
        val countAfterSecond = ARGBPaletteRegistry.getAllPalettes().size

        ARGBPaletteRegistry.reset(includeBuiltIn = true)
        val countAfterThird = ARGBPaletteRegistry.getAllPalettes().size

        // All counts should be the same
        assertEquals(initialCount, countAfterFirst)
        assertEquals(countAfterFirst, countAfterSecond)
        assertEquals(countAfterSecond, countAfterThird)

        // Built-in palettes should still exist
        assertNotNull(ARGBPaletteRegistry.getPalette("Rainbow"))
    }

    @Test
    fun `test registry maintains built-in palette consistency`() {
        // Get initial built-in palettes
        val initialBuiltInNames = ARGBPaletteRegistry.getPaletteNames().toSet()

        // Add custom palette
        ARGBPaletteRegistry.register(
            ARGBGradientPalette("Temporary", ARGBColor.RED, ARGBColor.GREEN)
        )

        // Reset
        ARGBPaletteRegistry.reset(includeBuiltIn = true)

        // Should have exactly the same built-in palettes
        val resetBuiltInNames = ARGBPaletteRegistry.getPaletteNames().toSet()
        assertEquals(initialBuiltInNames, resetBuiltInNames)
    }

    @Test
    fun `test concurrent modifications don't break registry`() {
        // This tests that the registry handles rapid modifications gracefully
        val palettes = (1..10).map { i ->
            ARGBGradientPalette(
                "Concurrent Test $i",
                ARGBColor(255, i * 25, 0, 0),
                ARGBColor(255, 0, i * 25, 0)
            )
        }

        // Register all rapidly
        palettes.forEach { ARGBPaletteRegistry.register(it) }

        // All should be retrievable
        palettes.forEach { palette ->
            assertNotNull(ARGBPaletteRegistry.getPalette(palette.name))
        }

        // Reset should clear them all
        ARGBPaletteRegistry.reset(includeBuiltIn = true)

        // None should remain
        palettes.forEach { palette ->
            assertNull(ARGBPaletteRegistry.getPalette(palette.name))
        }
    }

    @Test
    fun `test built-in palettes are properly initialized after reset`() {
        // Expected built-in palette names
        val expectedBuiltInPalettes = setOf(
            "Rainbow",
            "Rainbow (Transparent)",
            "Fire",
            "Fire (with Smoke)",
            "Cool Blue",
            "Cool Blue (Ice)",
            "Plasma",
            "Plasma (Energy)",
            "Red-Yellow",
            "Purple-Pink",
            "Green-Cyan",
            "Fade to Transparent",
            "Ghost White"
        )

        // Reset to ensure clean state
        ARGBPaletteRegistry.reset(includeBuiltIn = true)

        // Check all expected palettes exist
        val actualPalettes = ARGBPaletteRegistry.getPaletteNames().toSet()
        assertTrue(
            actualPalettes.containsAll(expectedBuiltInPalettes),
            "Missing palettes: ${expectedBuiltInPalettes - actualPalettes}"
        )

        // Verify each palette works correctly
        expectedBuiltInPalettes.forEach { name ->
            val palette = ARGBPaletteRegistry.getPalette(name)
            assertNotNull(palette, "Palette $name should exist")

            // Test that the palette produces valid colors
            val color = palette!!.getColor(100, 255)
            assertNotNull(color)
            assertTrue(color.alpha in 0..255)
            assertTrue(color.red in 0..255)
            assertTrue(color.green in 0..255)
            assertTrue(color.blue in 0..255)
        }
    }
}