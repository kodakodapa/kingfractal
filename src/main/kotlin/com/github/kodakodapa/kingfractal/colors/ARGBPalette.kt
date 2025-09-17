package com.github.kodakodapa.kingfractal.colors

import github.kodakodapa.kingfractal.colors.ARGBColorMatrix

import github.kodakodapa.kingfractal.colors.ARGBColor
import github.kodakodapa.kingfractal.colors.ARGBInterpolation

/**
 * Interface for ARGB-based color palettes with full alpha support
 * Supports varying palettes with complete [255][4] ARGB vectors
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
     * Generate a full [255][4] color matrix for this palette
     * This provides the requested [255][4] size vectors functionality
     */
    fun generateColorMatrix(maxIterations: Int = 255): ARGBColorMatrix {
        val matrix = ARGBColorMatrix(255)
        for (i in 0 until 255) {
            matrix[i] = getColor(i, maxIterations)
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
        if (iterations >= maxIterations) return ARGBColor.BLACK // Points in the set

        val t = iterations.toFloat() / maxIterations

        return if (useHSVInterpolation) {
            ARGBInterpolation.lerpHSV(startColor, endColor, t)
        } else {
            ARGBInterpolation.lerp(startColor, endColor, t)
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
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val hue = (iterations.toFloat() / maxIterations) * 360f
        val rgb = hsvToRgb(hue, 1f, 1f)

        val alpha = if (enableTransparency) {
            // Create transparency effect based on iteration distance
            val alphaFactor = iterations.toFloat() / maxIterations
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
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = iterations.toFloat() / maxIterations

        return when {
            t < 0.33f -> {
                // Black to red transition
                val intensity = (t / 0.33f * 255).toInt().coerceIn(0, 255)
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
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = iterations.toFloat() / maxIterations
        val blue = (t * 255).toInt().coerceIn(0, 255)
        val green = (t * 128).toInt().coerceIn(0, 255)

        val alpha = if (enableIceEffect) {
            // Create ice-like transparency effect
            val iceAlpha = (128 + t * 127).toInt().coerceIn(128, 255)
            iceAlpha
        } else {
            255
        }

        return ARGBColor(alpha, 0, green, blue)
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
        if (iterations >= maxIterations) return ARGBColor.BLACK

        val t = iterations.toFloat() / maxIterations

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

        return ARGBColor(alpha, r, g, b)
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
        if (iterations >= maxIterations) return ARGBColor.BLACK

        var finalR = 0f
        var finalG = 0f
        var finalB = 0f
        var finalA = 0f

        for ((palette, opacity) in layers) {
            val color = palette.getColor(iterations, maxIterations)
            val normalizedAlpha = (color.alpha / 255f) * opacity

            finalR += color.red * normalizedAlpha
            finalG += color.green * normalizedAlpha
            finalB += color.blue * normalizedAlpha
            finalA += normalizedAlpha
        }

        // Normalize by total alpha to prevent oversaturation
        if (finalA > 0) {
            finalR /= finalA
            finalG /= finalA
            finalB /= finalA
        }

        return ARGBColor(
            (finalA * 255).toInt().coerceIn(0, 255),
            finalR.toInt().coerceIn(0, 255),
            finalG.toInt().coerceIn(0, 255),
            finalB.toInt().coerceIn(0, 255)
        )
    }
}

/**
 * Registry for ARGB palettes with support for transparency and [255][4] vectors
 */
object ARGBPaletteRegistry {
    private val palettes = mutableMapOf<String, ARGBPalette>()

    init {
        // Register built-in palettes
        register(ARGBRainbowPalette(false))
        register(ARGBRainbowPalette(true))
        register(ARGBFirePalette(false))
        register(ARGBFirePalette(true))
        register(ARGBCoolBluePalette(false))
        register(ARGBCoolBluePalette(true))
        register(ARGBPlasmaPalette(false))
        register(ARGBPlasmaPalette(true))

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
     * Generate a full [255][4] matrix for all palettes
     * Returns a map of palette names to their color matrices
     */
    fun generateAllColorMatrices(maxIterations: Int = 255): Map<String, ARGBColorMatrix> {
        return palettes.mapValues { (_, palette) ->
            palette.generateColorMatrix(maxIterations)
        }
    }
}