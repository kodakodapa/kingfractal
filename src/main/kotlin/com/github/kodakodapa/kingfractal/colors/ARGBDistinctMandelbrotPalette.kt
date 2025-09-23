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
         * Generates a single distinct color for the given index using a multi-dimensional
         * space-filling approach that maximizes perceptual distance between consecutive indices
         */
        private fun generateDistinctColor(index: Int): ARGBColor {
            // Use golden ratio-based hue distribution for maximum separation
            val baseHue = (index * HUE_STEP) % 360f

            // Create variation patterns that cycle at different rates to avoid clustering
            val hueOffset = (index * 13.0f) % 60f - 30f  // ±30 degree variation
            val satCycle = sin(index * 0.031f) * 0.4f + 0.6f  // 0.2 to 1.0 saturation
            val valCycle = cos(index * 0.017f) * 0.3f + 0.7f  // 0.4 to 1.0 brightness

            // Apply color wheel shift based on index to maximize distinction
            val shiftedHue = (baseHue + hueOffset + (index / 100) * 7f) % 360f

            // Convert HSV to RGB
            val rgb = hsvToRgb(shiftedHue, satCycle, valCycle)

            // Apply perceptual enhancement to make colors more distinct
            val enhancedRgb = enhanceColorDistinction(rgb, index)

            return ARGBColor(255, enhancedRgb[0], enhancedRgb[1], enhancedRgb[2])
        }

        /**
         * Applies perceptual enhancement to make colors more visually distinct
         */
        private fun enhanceColorDistinction(rgb: IntArray, index: Int): IntArray {
            var r = rgb[0]
            var g = rgb[1]
            var b = rgb[2]

            // Apply gamma correction for better perceptual uniformity
            val gamma = 0.8f
            r = (255 * (r / 255.0).pow(gamma.toDouble())).toInt().coerceIn(0, 255)
            g = (255 * (g / 255.0).pow(gamma.toDouble())).toInt().coerceIn(0, 255)
            b = (255 * (b / 255.0).pow(gamma.toDouble())).toInt().coerceIn(0, 255)

            // Add deterministic index-based color shifts to break up similar colors
            val rShift = ((index * 7) % 31) - 15  // ±15 shift
            val gShift = ((index * 11) % 31) - 15
            val bShift = ((index * 13) % 31) - 15

            r = (r + rShift).coerceIn(0, 255)
            g = (g + gShift).coerceIn(0, 255)
            b = (b + bShift).coerceIn(0, 255)

            // Apply additional contrast enhancement without recursive lookups
            if (index % 2 == 1) {
                // Alternate enhancement for odd indices to increase distinction
                r = ((r + 128) % 256)
                g = ((g + 85) % 256)
                b = ((b + 171) % 256)
            }

            return intArrayOf(r, g, b)
        }


        /**
         * Converts HSV to RGB with proper handling of edge cases
         */
        private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
            val hue = h % 360f
            val sat = s.coerceIn(0f, 1f)
            val value = v.coerceIn(0f, 1f)

            val c = value * sat
            val x = c * (1 - abs((hue / 60f) % 2 - 1))
            val m = value - c

            val (r1, g1, b1) = when {
                hue < 60f -> Triple(c, x, 0f)
                hue < 120f -> Triple(x, c, 0f)
                hue < 180f -> Triple(0f, c, x)
                hue < 240f -> Triple(0f, x, c)
                hue < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
            val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
            val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

            return intArrayOf(r, g, b)
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