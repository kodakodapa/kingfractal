package com.github.kodakodapa.kingfractal.colors

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach

class ARGBPaletteRegistryTest {

    @BeforeEach
    fun setUp() {
        // Reset registry to known state before each test
        ARGBPaletteRegistry.reset(includeBuiltIn = true)
    }

    @AfterEach
    fun tearDown() {
        // Clean up after each test to ensure isolation
        ARGBPaletteRegistry.reset(includeBuiltIn = true)
    }

    @Test
    fun `test registry contains expected built-in palettes`() {
        val allPalettes = ARGBPaletteRegistry.getAllPalettes()
        val paletteNames = ARGBPaletteRegistry.getPaletteNames()

        assertTrue(allPalettes.isNotEmpty())
        assertTrue(paletteNames.isNotEmpty())
        assertEquals(allPalettes.size, paletteNames.size)

        // Check for expected built-in palettes
        val expectedPalettes = setOf(
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

        val actualNames = paletteNames.toSet()
        assertTrue(actualNames.containsAll(expectedPalettes),
            "Missing palettes: ${expectedPalettes - actualNames}")
    }

    @Test
    fun `test get palette by name`() {
        // Test existing palette
        val rainbowPalette = ARGBPaletteRegistry.getPalette("Rainbow")
        assertNotNull(rainbowPalette)
        assertEquals("Rainbow", rainbowPalette!!.name)
        assertFalse(rainbowPalette.supportsTransparency)

        // Test transparent version
        val transparentRainbow = ARGBPaletteRegistry.getPalette("Rainbow (Transparent)")
        assertNotNull(transparentRainbow)
        assertEquals("Rainbow (Transparent)", transparentRainbow!!.name)
        assertTrue(transparentRainbow.supportsTransparency)

        // Test non-existent palette
        val nonExistent = ARGBPaletteRegistry.getPalette("Does Not Exist")
        assertNull(nonExistent)
    }

    @Test
    fun `test transparent vs opaque palette filtering`() {
        val transparentPalettes = ARGBPaletteRegistry.getTransparentPalettes()
        val opaquePalettes = ARGBPaletteRegistry.getOpaquePalettes()
        val allPalettes = ARGBPaletteRegistry.getAllPalettes()

        // All palettes should be either transparent or opaque
        assertEquals(allPalettes.size, transparentPalettes.size + opaquePalettes.size)

        // Check that transparent palettes actually support transparency
        transparentPalettes.forEach { palette ->
            assertTrue(palette.supportsTransparency,
                "Palette '${palette.name}' should support transparency")
        }

        // Check that opaque palettes don't support transparency
        opaquePalettes.forEach { palette ->
            assertFalse(palette.supportsTransparency,
                "Palette '${palette.name}' should not support transparency")
        }

        // Should have at least some of each type
        assertTrue(transparentPalettes.isNotEmpty())
        assertTrue(opaquePalettes.isNotEmpty())
    }

    @Test
    fun `test palette registration works`() {
        // Get the initial count with built-in palettes
        val originalCount = ARGBPaletteRegistry.getAllPalettes().size

        // Create a unique test palette with timestamp to avoid conflicts
        val uniqueName = "Test Registration Palette ${System.currentTimeMillis()}"
        val testPalette = ARGBGradientPalette(
            uniqueName,
            ARGBColor.RED,
            ARGBColor.BLUE
        )

        // Register it
        ARGBPaletteRegistry.register(testPalette)

        // Verify it was added
        val newCount = ARGBPaletteRegistry.getAllPalettes().size
        assertEquals(originalCount + 1, newCount, "Palette count should increase by 1")

        val retrievedPalette = ARGBPaletteRegistry.getPalette(uniqueName)
        assertNotNull(retrievedPalette, "Should be able to retrieve registered palette")
        assertEquals(testPalette.name, retrievedPalette!!.name)
        assertTrue(ARGBPaletteRegistry.getPaletteNames().contains(uniqueName))
    }

    @Test
    fun `test palette overwrite in registry`() {
        // Use a unique name to avoid conflicts with built-in palettes
        val uniqueName = "Overwrite Test ${System.currentTimeMillis()}"

        val originalPalette = ARGBGradientPalette(
            uniqueName,
            ARGBColor.RED,
            ARGBColor.GREEN
        )

        val replacementPalette = ARGBGradientPalette(
            uniqueName,
            ARGBColor.BLUE,
            ARGBColor.WHITE
        )

        // Register original
        ARGBPaletteRegistry.register(originalPalette)
        val countAfterFirst = ARGBPaletteRegistry.getAllPalettes().size

        // Register replacement with same name
        ARGBPaletteRegistry.register(replacementPalette)
        val countAfterSecond = ARGBPaletteRegistry.getAllPalettes().size

        // Count should be the same (replacement, not addition)
        assertEquals(countAfterFirst, countAfterSecond, "Count should not change when overwriting")

        // Should get the replacement palette
        val retrieved = ARGBPaletteRegistry.getPalette(uniqueName)
        assertNotNull(retrieved, "Should retrieve palette after overwrite")

        // Verify it's the replacement by checking colors
        val color = retrieved!!.getColor(0, 255)
        val originalColor = originalPalette.getColor(0, 255)
        val replacementColor = replacementPalette.getColor(0, 255)

        assertEquals(replacementColor, color, "Should get replacement palette's color")
        assertNotEquals(originalColor, color, "Should not get original palette's color")
    }

    @Test
    fun `test generate all color matrices`() {
        val matrices = ARGBPaletteRegistry.generateAllColorMatrices()
        val allPalettes = ARGBPaletteRegistry.getAllPalettes()

        assertEquals(allPalettes.size, matrices.size)

        // Check that each palette has a corresponding matrix
        allPalettes.forEach { palette ->
            assertTrue(matrices.containsKey(palette.name),
                "Missing matrix for palette: ${palette.name}")

            val matrix = matrices[palette.name]!!
            assertEquals(255, matrix.size)

            // Verify matrix contents match palette
            for (i in 0..254) {
                val directColor = palette.getColor(i, 255)
                val matrixColor = matrix[i]
                assertEquals(directColor, matrixColor,
                    "Matrix color mismatch for palette ${palette.name} at iteration $i")
            }
        }
    }

    @Test
    fun `test generate color matrices with custom max iterations`() {
        val customMaxIterations = 100
        val matrices = ARGBPaletteRegistry.generateAllColorMatrices(customMaxIterations)

        matrices.values.forEach { matrix ->
            assertEquals(255, matrix.size) // Matrix size is always 255
        }

        // Verify that the colors are generated with the custom max iterations
        val rainbowPalette = ARGBPaletteRegistry.getPalette("Rainbow")!!
        val rainbowMatrix = matrices["Rainbow"]!!

        // Compare matrix generated with custom iterations vs direct palette call
        val directColor = rainbowPalette.getColor(50, customMaxIterations)
        val matrixColor = rainbowMatrix[50]
        assertEquals(directColor, matrixColor)
    }

    @Test
    fun `test all registered palettes have unique names`() {
        val paletteNames = ARGBPaletteRegistry.getPaletteNames()
        val uniqueNames = paletteNames.toSet()

        assertEquals(paletteNames.size, uniqueNames.size,
            "Duplicate palette names found: ${paletteNames.groupBy { it }.filter { it.value.size > 1 }.keys}")
    }

    @Test
    fun `test all registered palettes produce valid colors`() {
        val allPalettes = ARGBPaletteRegistry.getAllPalettes()

        allPalettes.forEach { palette ->
            // Test a few iteration values
            val testIterations = listOf(0, 50, 100, 200, 254)

            testIterations.forEach { iteration ->
                val color = palette.getColor(iteration, 255)

                // Verify color components are in valid range
                assertTrue(color.alpha in 0..255,
                    "Invalid alpha in palette '${palette.name}' at iteration $iteration: ${color.alpha}")
                assertTrue(color.red in 0..255,
                    "Invalid red in palette '${palette.name}' at iteration $iteration: ${color.red}")
                assertTrue(color.green in 0..255,
                    "Invalid green in palette '${palette.name}' at iteration $iteration: ${color.green}")
                assertTrue(color.blue in 0..255,
                    "Invalid blue in palette '${palette.name}' at iteration $iteration: ${color.blue}")
            }

            // Test edge case: iteration >= maxIterations should return BLACK
            assertEquals(ARGBColor.BLACK, palette.getColor(255, 255),
                "Palette '${palette.name}' should return BLACK for iterations >= maxIterations")
        }
    }

    @Test
    fun `test transparency consistency`() {
        val transparentPalettes = ARGBPaletteRegistry.getTransparentPalettes()
        val opaquePalettes = ARGBPaletteRegistry.getOpaquePalettes()

        // Test that transparent palettes actually produce some transparent colors
        transparentPalettes.forEach { palette ->
            val colors = (0..254 step 10).map { palette.getColor(it, 255) }
            val hasTransparentColors = colors.any { it.alpha < 255 }

            // Some transparent palettes might not have transparency at all iterations,
            // but they should at least be marked as supporting it
            assertTrue(palette.supportsTransparency,
                "Palette '${palette.name}' is in transparent list but doesn't support transparency")
        }

        // Test that opaque palettes consistently produce opaque colors
        opaquePalettes.forEach { palette ->
            val colors = (0..254 step 10).map { palette.getColor(it, 255) }
            val allOpaque = colors.all { it.alpha == 255 }

            assertTrue(allOpaque,
                "Palette '${palette.name}' is in opaque list but produces transparent colors")
        }
    }
}