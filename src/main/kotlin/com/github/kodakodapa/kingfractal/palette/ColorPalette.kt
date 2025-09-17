package com.github.kodakodapa.kingfractal.palette

/**
 * Represents a color palette for fractal rendering
 */
interface ColorPalette {
    /**
     * Get the RGB color for a given iteration value
     * @param iterations The number of iterations (0 to maxIterations)
     * @param maxIterations The maximum number of iterations
     * @return RGB color as an integer (0xRRGGBB)
     */
    fun getColor(iterations: Int, maxIterations: Int): Int
    
    /**
     * Name of the palette
     */
    val name: String
}

/**
 * A simple gradient palette between two colors
 */
class GradientPalette(
    override val name: String,
    private val startColor: Int,
    private val endColor: Int
) : ColorPalette {
    
    override fun getColor(iterations: Int, maxIterations: Int): Int {
        if (iterations >= maxIterations) return 0x000000 // Black for points in the set
        
        val t = iterations.toFloat() / maxIterations
        
        val startR = (startColor shr 16) and 0xFF
        val startG = (startColor shr 8) and 0xFF
        val startB = startColor and 0xFF
        
        val endR = (endColor shr 16) and 0xFF
        val endG = (endColor shr 8) and 0xFF
        val endB = endColor and 0xFF
        
        val r = (startR + t * (endR - startR)).toInt().coerceIn(0, 255)
        val g = (startG + t * (endG - startG)).toInt().coerceIn(0, 255)
        val b = (startB + t * (endB - startB)).toInt().coerceIn(0, 255)
        
        return (r shl 16) or (g shl 8) or b
    }
}

/**
 * A rainbow spectrum palette
 */
class RainbowPalette : ColorPalette {
    override val name = "Rainbow"
    
    override fun getColor(iterations: Int, maxIterations: Int): Int {
        if (iterations >= maxIterations) return 0x000000 // Black for points in the set
        
        val hue = (iterations.toFloat() / maxIterations) * 360f
        return hsvToRgb(hue, 1f, 1f)
    }
    
    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
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
        
        return (r shl 16) or (g shl 8) or b
    }
}

/**
 * A fire-themed palette with reds, oranges, and yellows
 */
class FirePalette : ColorPalette {
    override val name = "Fire"
    
    override fun getColor(iterations: Int, maxIterations: Int): Int {
        if (iterations >= maxIterations) return 0x000000 // Black for points in the set
        
        val t = iterations.toFloat() / maxIterations
        
        return when {
            t < 0.33f -> {
                // Black to red
                val intensity = (t / 0.33f * 255).toInt().coerceIn(0, 255)
                (intensity shl 16)
            }
            t < 0.66f -> {
                // Red to orange
                val intensity = ((t - 0.33f) / 0.33f * 255).toInt().coerceIn(0, 255)
                (255 shl 16) or (intensity shl 8)
            }
            else -> {
                // Orange to yellow
                val intensity = ((t - 0.66f) / 0.34f * 255).toInt().coerceIn(0, 255)
                (255 shl 16) or (255 shl 8) or intensity
            }
        }
    }
}

/**
 * A cool blue-themed palette
 */
class CoolBluePalette : ColorPalette {
    override val name = "Cool Blue"
    
    override fun getColor(iterations: Int, maxIterations: Int): Int {
        if (iterations >= maxIterations) return 0x000000 // Black for points in the set
        
        val t = iterations.toFloat() / maxIterations
        val blue = (t * 255).toInt().coerceIn(0, 255)
        val green = (t * 128).toInt().coerceIn(0, 255)
        
        return (green shl 8) or blue
    }
}

/**
 * Registry for available palettes
 */
object PaletteRegistry {
    private val palettes = mutableMapOf<String, ColorPalette>()
    
    init {
        register(RainbowPalette())
        register(FirePalette())
        register(CoolBluePalette())
        register(GradientPalette("Red-Yellow", 0xFF0000, 0xFFFF00))
        register(GradientPalette("Purple-Pink", 0x800080, 0xFF69B4))
        register(GradientPalette("Green-Cyan", 0x008000, 0x00FFFF))
    }
    
    fun register(palette: ColorPalette) {
        palettes[palette.name] = palette
    }
    
    fun getPalette(name: String): ColorPalette? = palettes[name]
    
    fun getAllPalettes(): List<ColorPalette> = palettes.values.toList()
    
    fun getPaletteNames(): List<String> = palettes.keys.toList()
}