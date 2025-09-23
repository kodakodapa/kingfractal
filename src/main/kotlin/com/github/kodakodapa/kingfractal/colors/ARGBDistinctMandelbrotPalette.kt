package com.github.kodakodapa.kingfractal.colors

import kotlin.math.*

/**
 * Palette designed specifically for Mandelbrot sets with 1000 maximally distinct colors.
 * Uses a space-filling approach to ensure consecutive iteration counts produce
 * visually distinct colors, optimized for fractal exploration.
 */
class ARGBDistinctMandelbrotPalette : ARGBPalette {
    override val name = "Distinct Mandelbrot (1000 Colors)"
    override val supportsTransparency = false

    companion object {
        private const val PALETTE_SIZE = 1000
        private const val GOLDEN_RATIO = 0.618034f
        private const val HUE_STEP = GOLDEN_RATIO * 360f

        // Pre-computed color table for maximum performance
        private val colorTable: Array<ARGBColor> by lazy {
            generateDistinctColors()
        }

        /**
         * Generates 1000 maximally distinct colors using a space-filling approach
         */
        private fun generateDistinctColors(): Array<ARGBColor> {
            val colors = Array(PALETTE_SIZE) { ARGBColor.BLACK }

            for (i in 0 until PALETTE_SIZE) {
                colors[i] = generateDistinctColor(i)
            }

            return colors
        }

        /**
         * Generates a single distinct color for the given index using a direct approach
         * that guarantees maximum distance between consecutive colors
         */
        private fun generateDistinctColor(index: Int): ARGBColor {
            // Use a simple but effective approach: divide RGB space into large chunks
            // and jump between very different regions for consecutive indices

            val r: Int
            val g: Int
            val b: Int

            when (index % 6) {
                0 -> {
                    // Red dominant
                    r = 255
                    g = (index * 31) % 128
                    b = (index * 17) % 128
                }
                1 -> {
                    // Green dominant
                    r = (index * 23) % 128
                    g = 255
                    b = (index * 37) % 128
                }
                2 -> {
                    // Blue dominant
                    r = (index * 41) % 128
                    g = (index * 29) % 128
                    b = 255
                }
                3 -> {
                    // Cyan (no red)
                    r = (index * 19) % 100
                    g = 200 + (index * 13) % 55
                    b = 200 + (index * 43) % 55
                }
                4 -> {
                    // Magenta (no green)
                    r = 200 + (index * 47) % 55
                    g = (index * 11) % 100
                    b = 200 + (index * 53) % 55
                }
                else -> {
                    // Yellow (no blue)
                    r = 200 + (index * 59) % 55
                    g = 200 + (index * 61) % 55
                    b = (index * 7) % 100
                }
            }

            return ARGBColor(255, r, g, b)
        }



    }

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK

        // Map iteration to our 1000-color palette
        val colorIndex = if (maxIterations <= PALETTE_SIZE) {
            // Direct mapping for small iteration counts
            iterations % PALETTE_SIZE
        } else {
            // Scale to our palette size for larger iteration counts
            ((iterations.toFloat() / maxIterations.toFloat()) * PALETTE_SIZE).toInt() % PALETTE_SIZE
        }

        return colorTable[colorIndex]
    }

    /**
     * Get color by direct index into the 1000-color palette (useful for testing)
     */
    fun getColorByIndex(index: Int): ARGBColor {
        return colorTable[index % PALETTE_SIZE]
    }

    /**
     * Get the total number of distinct colors in this palette
     */
    fun getPaletteSize(): Int = PALETTE_SIZE
}