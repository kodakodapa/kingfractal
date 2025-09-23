package com.github.kodakodapa.kingfractal.colors

import com.github.kodakodapa.kingfractal.colors.ARGBColor
import com.github.kodakodapa.kingfractal.colors.ARGBColorMatrix
import com.github.kodakodapa.kingfractal.colors.ARGBInterpolation

/**
 * Interface for ARGB-based color palettes with full alpha support
 * Supports varying palettes with complete [256][4] ARGB vectors
 */
interface ARGBPalette {
    /**
     * Get the ARGB color for a given iteration value
     * @param iterations The number of iterations (0 to maxIterations)
     * @param maxIterations The maximum number of iterations
     * @return ARGB color with full alpha support
     */
    fun getColor(iterations: Int, maxIterations: Int): ARGBColor

    /**
     * Get the color as a 4-component vector [A, R, G, B]
     */
    fun getColorVector(iterations: Int, maxIterations: Int): IntArray =
        getColor(iterations, maxIterations).toVector()

    /**
     * Name of the palette
     */
    val name: String

    /**
     * Whether this palette supports transparency
     */
    val supportsTransparency: Boolean get() = false

    /**
     * Generate a full [256][4] color matrix for this palette
     * This provides the requested [256][4] size vectors functionality
     */
    fun generateColorMatrix(maxIterations: Int = 255): ARGBColorMatrix {
        val validMaxIterations = maxIterations.coerceAtLeast(1)
        val matrix = ARGBColorMatrix(255)

        try {
            for (i in 0..255) {
                matrix[i] = getColor(i, validMaxIterations)
            }
        } catch (e: Exception) {
            // If generation fails, fill with fallback gradient
            matrix.fillGradient(ARGBColor.BLACK, ARGBColor.WHITE)
        }

        return matrix
    }
}

/**
 * Simple gradient palette between two ARGB colors with full alpha support
 */
class ARGBGradientPalette(
    override val name: String,
    private val startColor: ARGBColor,
    private val endColor: ARGBColor,
    private val useHSVInterpolation: Boolean = false
) : ARGBPalette {

    override val supportsTransparency: Boolean =
        startColor.alpha < 255 || endColor.alpha < 255

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return startColor
        if (iterations >= maxIterations) return ARGBColor.BLACK // Points in the set

        val t = if (maxIterations == 1) {
            0f // Avoid division by zero when maxIterations is 1
        } else {
            iterations.toFloat() / maxIterations.toFloat()
        }

        return try {
            if (useHSVInterpolation) {
                ARGBInterpolation.lerpHSV(startColor, endColor, t)
            } else {
                ARGBInterpolation.lerp(startColor, endColor, t)
            }
        } catch (e: Exception) {
            // Fallback to start color if interpolation fails
            startColor
        }
    }
}

/**
 * Rainbow spectrum palette with optional transparency effects
 */
class ARGBRainbowPalette(
    private val enableTransparency: Boolean = false,
    private val minAlpha: Int = 128
) : ARGBPalette {
    override val name = if (enableTransparency) "Rainbow (Transparent)" else "Rainbow"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val hue = if (maxIterations == 1) {
            0f // Avoid division by zero
        } else {
            (iterations.toFloat() / maxIterations.toFloat()) * 360f
        }

        val rgb = try {
            hsvToRgb(hue, 1f, 1f)
        } catch (e: Exception) {
            intArrayOf(0, 0, 0) // Fallback to black RGB
        }

        val alpha = if (enableTransparency) {
            // Create transparency effect based on iteration distance
            val alphaFactor = if (maxIterations == 1) {
                0f
            } else {
                iterations.toFloat() / maxIterations.toFloat()
            }
            (minAlpha + (255 - minAlpha) * alphaFactor).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, rgb[0], rgb[1], rgb[2])
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): IntArray {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((r1 + m) * 255).toInt().coerceIn(0, 255)
        val g = ((g1 + m) * 255).toInt().coerceIn(0, 255)
        val b = ((b1 + m) * 255).toInt().coerceIn(0, 255)

        return intArrayOf(r, g, b)
    }
}

/**
 * Fire-themed palette with smoke transparency effects
 */
class ARGBFirePalette(
    private val enableSmoke: Boolean = false
) : ARGBPalette {
    override val name = if (enableSmoke) "Fire (with Smoke)" else "Fire"
    override val supportsTransparency = enableSmoke

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = if (maxIterations == 1) {
            0f // Avoid division by zero
        } else {
            iterations.toFloat() / maxIterations.toFloat()
        }

        return try {
            when {
                t < 0.33f -> {
                    // Black to red transition
                    val intensity = if (t == 0f) 0 else ((t / 0.33f) * 255).toInt().coerceIn(0, 255)
                    val alpha = if (enableSmoke) (intensity * 0.8f).toInt().coerceIn(50, 255) else 255
                    ARGBColor(alpha, intensity, 0, 0)
                }
                t < 0.66f -> {
                    // Red to orange transition
                    val intensity = ((t - 0.33f) / 0.33f * 255).toInt().coerceIn(0, 255)
                    val alpha = if (enableSmoke) (200 + intensity * 0.2f).toInt().coerceIn(200, 255) else 255
                    ARGBColor(alpha, 255, intensity, 0)
                }
                else -> {
                    // Orange to yellow transition with smoke effect
                    val intensity = ((t - 0.66f) / 0.34f * 255).toInt().coerceIn(0, 255)
                    val alpha = if (enableSmoke) {
                        // Create smoke effect at high temperatures
                        (255 - intensity * 0.3f).toInt().coerceIn(180, 255)
                    } else {
                        255
                    }
                    ARGBColor(alpha, 255, 255, intensity)
                }
            }
        } catch (e: Exception) {
            // Fallback to red color for fire palette
            ARGBColor(255, 255, 0, 0)
        }
    }
}

/**
 * Cool blue palette with ice transparency effects
 */
class ARGBCoolBluePalette(
    private val enableIceEffect: Boolean = false
) : ARGBPalette {
    override val name = if (enableIceEffect) "Cool Blue (Ice)" else "Cool Blue"
    override val supportsTransparency = enableIceEffect

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = if (maxIterations == 1) {
            0f // Avoid division by zero
        } else {
            iterations.toFloat() / maxIterations.toFloat()
        }

        return try {
            val blue = (t * 255).toInt().coerceIn(0, 255)
            val green = (t * 128).toInt().coerceIn(0, 255)

            val alpha = if (enableIceEffect) {
                // Create ice-like transparency effect
                val iceAlpha = (128 + t * 127).toInt().coerceIn(128, 255)
                iceAlpha
            } else {
                255
            }

            ARGBColor(alpha, 0, green, blue)
        } catch (e: Exception) {
            // Fallback to blue color
            ARGBColor(255, 0, 0, 255)
        }
    }
}

/**
 * Plasma palette with energy transparency effects
 */
class ARGBPlasmaPalette(
    private val enableEnergyEffect: Boolean = false
) : ARGBPalette {
    override val name = if (enableEnergyEffect) "Plasma (Energy)" else "Plasma"
    override val supportsTransparency = enableEnergyEffect

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = if (maxIterations == 1) {
            0f // Avoid division by zero
        } else {
            iterations.toFloat() / maxIterations.toFloat()
        }

        return try {
            // Create plasma-like color cycling
            val phase1 = kotlin.math.sin(t * kotlin.math.PI * 2).toFloat()
            val phase2 = kotlin.math.sin(t * kotlin.math.PI * 3 + kotlin.math.PI / 3).toFloat()
            val phase3 = kotlin.math.sin(t * kotlin.math.PI * 5 + kotlin.math.PI * 2 / 3).toFloat()

            val r = ((phase1 + 1) * 127.5f).toInt().coerceIn(0, 255)
            val g = ((phase2 + 1) * 127.5f).toInt().coerceIn(0, 255)
            val b = ((phase3 + 1) * 127.5f).toInt().coerceIn(0, 255)

            val alpha = if (enableEnergyEffect) {
                // Pulsating energy effect
                val energyAlpha = ((kotlin.math.sin(t * kotlin.math.PI * 4) + 1) * 100 + 55).toInt().coerceIn(55, 255)
                energyAlpha
            } else {
                255
            }

            ARGBColor(alpha, r, g, b)
        } catch (e: Exception) {
            // Fallback to magenta for plasma
            ARGBColor(255, 255, 0, 255)
        }
    }
}

/**
 * Multi-layer palette that combines multiple palettes with different transparency levels
 */
class ARGBLayeredPalette(
    override val name: String,
    private val layers: List<Pair<ARGBPalette, Float>> // Palette and opacity multiplier
) : ARGBPalette {
    override val supportsTransparency = true

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        // Handle edge cases
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor.BLACK
        if (layers.isEmpty()) return ARGBColor.BLACK

        return try {
            var finalR = 0f
            var finalG = 0f
            var finalB = 0f
            var finalA = 0f

            for ((palette, opacity) in layers) {
                // Null safety and opacity validation
                if (palette == null || opacity <= 0f) continue

                val color = try {
                    palette.getColor(iterations, maxIterations)
                } catch (e: Exception) {
                    continue // Skip this layer if it fails
                }

                val clampedOpacity = opacity.coerceIn(0f, 1f)
                val normalizedAlpha = (color.alpha / 255f) * clampedOpacity

                finalR += color.red * normalizedAlpha
                finalG += color.green * normalizedAlpha
                finalB += color.blue * normalizedAlpha
                finalA += normalizedAlpha
            }

            // Normalize by total alpha to prevent oversaturation
            if (finalA > 0f) {
                finalR /= finalA
                finalG /= finalA
                finalB /= finalA
            } else {
                // If no valid layers, return black
                return ARGBColor.BLACK
            }

            ARGBColor(
                (finalA * 255).toInt().coerceIn(0, 255),
                finalR.toInt().coerceIn(0, 255),
                finalG.toInt().coerceIn(0, 255),
                finalB.toInt().coerceIn(0, 255)
            )
        } catch (e: Exception) {
            // Fallback to black if composition fails
            ARGBColor.BLACK
        }
    }
}

/**
 * Ocean depth palette with blue-green transitions
 */
class ARGBOceanPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Ocean (Transparent)" else "Ocean"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 0, 0, 20) // Deep ocean

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create ocean depth transition: light blue -> cyan -> deep blue -> dark blue
        val (r, g, b) = when {
            t < 0.25f -> {
                val factor = t / 0.25f
                val r = (173 * (1 - factor) + 0 * factor).toInt()
                val g = (216 * (1 - factor) + 191 * factor).toInt()
                val b = (230 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.5f -> {
                val factor = (t - 0.25f) / 0.25f
                val r = (0 * (1 - factor) + 0 * factor).toInt()
                val g = (191 * (1 - factor) + 128 * factor).toInt()
                val b = (255 * (1 - factor) + 128 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.75f -> {
                val factor = (t - 0.5f) / 0.25f
                val r = (0 * (1 - factor) + 25 * factor).toInt()
                val g = (128 * (1 - factor) + 25 * factor).toInt()
                val b = (128 * (1 - factor) + 112 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.75f) / 0.25f
                val r = (25 * (1 - factor) + 0 * factor).toInt()
                val g = (25 * (1 - factor) + 0 * factor).toInt()
                val b = (112 * (1 - factor) + 20 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (128 + (127 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Sunset/sunrise palette with warm orange-red transitions
 */
class ARGBSunsetPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Sunset (Transparent)" else "Sunset"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 25, 0, 25) // Deep purple

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create sunset transition: yellow -> orange -> red -> purple -> dark
        val (r, g, b) = when {
            t < 0.2f -> {
                val factor = t / 0.2f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (255 * (1 - factor) + 165 * factor).toInt()
                val b = (0 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.4f -> {
                val factor = (t - 0.2f) / 0.2f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (165 * (1 - factor) + 69 * factor).toInt()
                val b = (0 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.6f -> {
                val factor = (t - 0.4f) / 0.2f
                val r = (255 * (1 - factor) + 220 * factor).toInt()
                val g = (69 * (1 - factor) + 20 * factor).toInt()
                val b = (0 * (1 - factor) + 60 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.8f -> {
                val factor = (t - 0.6f) / 0.2f
                val r = (220 * (1 - factor) + 128 * factor).toInt()
                val g = (20 * (1 - factor) + 0 * factor).toInt()
                val b = (60 * (1 - factor) + 128 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.8f) / 0.2f
                val r = (128 * (1 - factor) + 25 * factor).toInt()
                val g = (0 * (1 - factor) + 0 * factor).toInt()
                val b = (128 * (1 - factor) + 25 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (100 + (155 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Electric neon palette with bright cyan-magenta transitions
 */
class ARGBElectricPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Electric (Transparent)" else "Electric"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 0, 0, 0) // Black

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create electric transition: black -> cyan -> white -> magenta -> purple
        val (r, g, b) = when {
            t < 0.25f -> {
                val factor = t / 0.25f
                val r = (0 * (1 - factor) + 0 * factor).toInt()
                val g = (0 * (1 - factor) + 255 * factor).toInt()
                val b = (0 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.5f -> {
                val factor = (t - 0.25f) / 0.25f
                val r = (0 * (1 - factor) + 255 * factor).toInt()
                val g = (255 * (1 - factor) + 255 * factor).toInt()
                val b = (255 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.75f -> {
                val factor = (t - 0.5f) / 0.25f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (255 * (1 - factor) + 0 * factor).toInt()
                val b = (255 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.75f) / 0.25f
                val r = (255 * (1 - factor) + 128 * factor).toInt()
                val g = (0 * (1 - factor) + 0 * factor).toInt()
                val b = (255 * (1 - factor) + 128 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (50 + (205 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Copper metallic palette with warm brown-orange tones
 */
class ARGBCopperPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Copper (Transparent)" else "Copper"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 101, 67, 33) // Dark copper

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create copper transition: black -> dark copper -> bright copper -> gold
        val (r, g, b) = when {
            t < 0.3f -> {
                val factor = t / 0.3f
                val r = (0 * (1 - factor) + 101 * factor).toInt()
                val g = (0 * (1 - factor) + 67 * factor).toInt()
                val b = (0 * (1 - factor) + 33 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.6f -> {
                val factor = (t - 0.3f) / 0.3f
                val r = (101 * (1 - factor) + 184 * factor).toInt()
                val g = (67 * (1 - factor) + 115 * factor).toInt()
                val b = (33 * (1 - factor) + 51 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.8f -> {
                val factor = (t - 0.6f) / 0.2f
                val r = (184 * (1 - factor) + 255 * factor).toInt()
                val g = (115 * (1 - factor) + 165 * factor).toInt()
                val b = (51 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.8f) / 0.2f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (165 * (1 - factor) + 215 * factor).toInt()
                val b = (0 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (80 + (175 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Grayscale palette with optional transparency and inversion
 */
class ARGBGrayscalePalette(
    private val enableTransparency: Boolean = false,
    private val inverted: Boolean = false
) : ARGBPalette {
    override val name = when {
        enableTransparency && inverted -> "Grayscale Inverted (Transparent)"
        enableTransparency -> "Grayscale (Transparent)"
        inverted -> "Grayscale Inverted"
        else -> "Grayscale"
    }
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return if (inverted) ARGBColor.WHITE else ARGBColor.BLACK

        val t = iterations.toFloat() / maxIterations.toFloat()
        val intensity = if (inverted) (255 * (1 - t)).toInt() else (255 * t).toInt()

        val alpha = if (enableTransparency) {
            (50 + (205 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, intensity, intensity, intensity)
    }
}

/**
 * Vibrant tropical palette with green-yellow-pink transitions
 */
class ARGBTropicalPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Tropical (Transparent)" else "Tropical"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 0, 100, 0) // Dark green

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create tropical transition: dark green -> lime -> yellow -> orange -> pink
        val (r, g, b) = when {
            t < 0.2f -> {
                val factor = t / 0.2f
                val r = (0 * (1 - factor) + 50 * factor).toInt()
                val g = (100 * (1 - factor) + 205 * factor).toInt()
                val b = (0 * (1 - factor) + 50 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.4f -> {
                val factor = (t - 0.2f) / 0.2f
                val r = (50 * (1 - factor) + 255 * factor).toInt()
                val g = (205 * (1 - factor) + 255 * factor).toInt()
                val b = (50 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.6f -> {
                val factor = (t - 0.4f) / 0.2f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (255 * (1 - factor) + 165 * factor).toInt()
                val b = (0 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.8f -> {
                val factor = (t - 0.6f) / 0.2f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (165 * (1 - factor) + 20 * factor).toInt()
                val b = (0 * (1 - factor) + 147 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.8f) / 0.2f
                val r = (255 * (1 - factor) + 199 * factor).toInt()
                val g = (20 * (1 - factor) + 21 * factor).toInt()
                val b = (147 * (1 - factor) + 133 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (60 + (195 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Autumn forest palette with rich browns, oranges, and reds
 */
class ARGBAutumnPalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Autumn (Transparent)" else "Autumn"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 101, 67, 33) // Dark brown

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create autumn transition: dark brown -> orange -> red -> bright orange -> yellow
        val (r, g, b) = when {
            t < 0.25f -> {
                val factor = t / 0.25f
                val r = (101 * (1 - factor) + 205 * factor).toInt()
                val g = (67 * (1 - factor) + 133 * factor).toInt()
                val b = (33 * (1 - factor) + 63 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.5f -> {
                val factor = (t - 0.25f) / 0.25f
                val r = (205 * (1 - factor) + 220 * factor).toInt()
                val g = (133 * (1 - factor) + 20 * factor).toInt()
                val b = (63 * (1 - factor) + 60 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.75f -> {
                val factor = (t - 0.5f) / 0.25f
                val r = (220 * (1 - factor) + 255 * factor).toInt()
                val g = (20 * (1 - factor) + 140 * factor).toInt()
                val b = (60 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.75f) / 0.25f
                val r = (255 * (1 - factor) + 255 * factor).toInt()
                val g = (140 * (1 - factor) + 255 * factor).toInt()
                val b = (0 * (1 - factor) + 0 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (70 + (185 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Ice and snow palette with cool blue-white tones
 */
class ARGBIcePalette(
    private val enableTransparency: Boolean = false
) : ARGBPalette {
    override val name = if (enableTransparency) "Ice (Transparent)" else "Ice"
    override val supportsTransparency = enableTransparency

    override fun getColor(iterations: Int, maxIterations: Int): ARGBColor {
        if (maxIterations <= 0) return ARGBColor.BLACK
        if (iterations < 0) return ARGBColor.BLACK
        if (iterations >= maxIterations) return ARGBColor(255, 25, 25, 112) // Midnight blue

        val t = iterations.toFloat() / maxIterations.toFloat()

        // Create ice transition: dark blue -> ice blue -> cyan -> white -> light blue
        val (r, g, b) = when {
            t < 0.2f -> {
                val factor = t / 0.2f
                val r = (25 * (1 - factor) + 70 * factor).toInt()
                val g = (25 * (1 - factor) + 130 * factor).toInt()
                val b = (112 * (1 - factor) + 180 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.4f -> {
                val factor = (t - 0.2f) / 0.2f
                val r = (70 * (1 - factor) + 0 * factor).toInt()
                val g = (130 * (1 - factor) + 191 * factor).toInt()
                val b = (180 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.6f -> {
                val factor = (t - 0.4f) / 0.2f
                val r = (0 * (1 - factor) + 224 * factor).toInt()
                val g = (191 * (1 - factor) + 255 * factor).toInt()
                val b = (255 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            t < 0.8f -> {
                val factor = (t - 0.6f) / 0.2f
                val r = (224 * (1 - factor) + 255 * factor).toInt()
                val g = (255 * (1 - factor) + 255 * factor).toInt()
                val b = (255 * (1 - factor) + 255 * factor).toInt()
                Triple(r, g, b)
            }
            else -> {
                val factor = (t - 0.8f) / 0.2f
                val r = (255 * (1 - factor) + 173 * factor).toInt()
                val g = (255 * (1 - factor) + 216 * factor).toInt()
                val b = (255 * (1 - factor) + 230 * factor).toInt()
                Triple(r, g, b)
            }
        }

        val alpha = if (enableTransparency) {
            (100 + (155 * t)).toInt().coerceIn(0, 255)
        } else {
            255
        }

        return ARGBColor(alpha, r, g, b)
    }
}

/**
 * Registry for ARGB palettes with support for transparency and [256][4] vectors
 */
object ARGBPaletteRegistry {
    private val palettes = mutableMapOf<String, ARGBPalette>()
    private var initialized = false

    init {
        initializeBuiltInPalettes()
    }

    private fun initializeBuiltInPalettes() {
        if (initialized) return

        // Register built-in palettes
        register(ARGBRainbowPalette(false))
        register(ARGBRainbowPalette(true))
        register(ARGBFirePalette(false))
        register(ARGBFirePalette(true))
        register(ARGBCoolBluePalette(false))
        register(ARGBCoolBluePalette(true))
        register(ARGBPlasmaPalette(false))
        register(ARGBPlasmaPalette(true))

        // Register new palettes
        register(ARGBOceanPalette(false))
        register(ARGBOceanPalette(true))
        register(ARGBSunsetPalette(false))
        register(ARGBSunsetPalette(true))
        register(ARGBElectricPalette(false))
        register(ARGBElectricPalette(true))
        register(ARGBCopperPalette(false))
        register(ARGBCopperPalette(true))
        register(ARGBGrayscalePalette(false, false))
        register(ARGBGrayscalePalette(false, true))
        register(ARGBGrayscalePalette(true, false))
        register(ARGBTropicalPalette(false))
        register(ARGBTropicalPalette(true))
        register(ARGBAutumnPalette(false))
        register(ARGBAutumnPalette(true))
        register(ARGBIcePalette(false))
        register(ARGBIcePalette(true))

        // Register specialized palettes
        register(ARGBDistinctMandelbrotPalette())

        // Register gradient palettes
        register(ARGBGradientPalette("Red-Yellow", ARGBColor.RED, ARGBColor(255, 255, 255, 0)))
        register(ARGBGradientPalette("Purple-Pink",
            ARGBColor(255, 128, 0, 128),
            ARGBColor(255, 255, 105, 180)))
        register(ARGBGradientPalette("Green-Cyan",
            ARGBColor.GREEN,
            ARGBColor(255, 0, 255, 255)))

        // Transparent gradients
        register(ARGBGradientPalette("Fade to Transparent",
            ARGBColor.RED,
            ARGBColor(0, 255, 0, 0)))
        register(ARGBGradientPalette("Ghost White",
            ARGBColor(50, 255, 255, 255),
            ARGBColor(200, 255, 255, 255)))

        initialized = true
    }

    /**
     * Reset the registry to its initial state (useful for testing)
     * @param includeBuiltIn if true, re-initializes built-in palettes after clearing
     */
    internal fun reset(includeBuiltIn: Boolean = true) {
        palettes.clear()
        initialized = false
        if (includeBuiltIn) {
            initializeBuiltInPalettes()
        }
    }

    fun register(palette: ARGBPalette) {
        palettes[palette.name] = palette
    }

    fun getPalette(name: String): ARGBPalette? = palettes[name]

    fun getAllPalettes(): List<ARGBPalette> = palettes.values.toList()

    fun getPaletteNames(): List<String> = palettes.keys.toList()

    fun getTransparentPalettes(): List<ARGBPalette> =
        palettes.values.filter { it.supportsTransparency }

    fun getOpaquePalettes(): List<ARGBPalette> =
        palettes.values.filter { !it.supportsTransparency }

    /**
     * Generate a full [256][4] matrix for all palettes
     * Returns a map of palette names to their color matrices
     */
    fun generateAllColorMatrices(maxIterations: Int = 255): Map<String, ARGBColorMatrix> {
        return palettes.mapValues { (_, palette) ->
            palette.generateColorMatrix(maxIterations)
        }
    }
}