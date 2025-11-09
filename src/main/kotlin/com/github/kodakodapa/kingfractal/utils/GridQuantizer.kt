package com.github.kodakodapa.kingfractal.utils

import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Utility class for applying grid-based "fuse bead" effects to images.
 * Quantizes images into a grid of colored cells with various rendering styles.
 */
object GridQuantizer {

    /**
     * Applies a grid effect to the input image, making it look like fuse beads.
     *
     * @param source The source image to quantize
     * @param options Grid rendering options
     * @return A new BufferedImage with the grid effect applied
     */
    fun applyGridEffect(source: BufferedImage, options: GridRenderOptions): BufferedImage {
        val width = source.width
        val height = source.height
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = result.createGraphics()

        // Enable anti-aliasing for smoother bead edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

        // Calculate cell size
        val cellWidth = width.toFloat() / options.gridSize
        val cellHeight = height.toFloat() / options.gridSize

        // Process each grid cell
        for (gridY in 0 until options.gridSize) {
            for (gridX in 0 until options.gridSize) {
                // Calculate pixel bounds for this cell
                val x1 = (gridX * cellWidth).toInt()
                val y1 = (gridY * cellHeight).toInt()
                val x2 = ((gridX + 1) * cellWidth).toInt().coerceAtMost(width)
                val y2 = ((gridY + 1) * cellHeight).toInt().coerceAtMost(height)

                // Sample the average color from the source image
                val avgColor = getAverageColor(source, x1, y1, x2, y2)

                // Optionally quantize the color
                val finalColor = if (options.quantizeColors) {
                    quantizeColor(avgColor, options.colorCount)
                } else {
                    avgColor
                }

                // Draw the bead
                drawBead(g2d, x1, y1, x2, y2, finalColor, options)
            }
        }

        // Draw grid lines if enabled
        if (options.showGridLines) {
            drawGridLines(g2d, width, height, options.gridSize, cellWidth, cellHeight)
        }

        g2d.dispose()
        return result
    }

    /**
     * Calculates the average color of a rectangular region in an image.
     */
    private fun getAverageColor(image: BufferedImage, x1: Int, y1: Int, x2: Int, y2: Int): Color {
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0

        for (y in y1 until y2) {
            for (x in x1 until x2) {
                val rgb = image.getRGB(x, y)
                totalR += (rgb shr 16) and 0xFF
                totalG += (rgb shr 8) and 0xFF
                totalB += rgb and 0xFF
                count++
            }
        }

        if (count == 0) return Color.BLACK

        return Color(
            (totalR / count).toInt(),
            (totalG / count).toInt(),
            (totalB / count).toInt()
        )
    }

    /**
     * Quantizes a color to one of N discrete colors using simple palette reduction.
     */
    private fun quantizeColor(color: Color, colorCount: Int): Color {
        val levels = when {
            colorCount <= 8 -> 2   // 2^3 = 8 colors
            colorCount <= 27 -> 3  // 3^3 = 27 colors
            colorCount <= 64 -> 4  // 4^3 = 64 colors
            colorCount <= 125 -> 5 // 5^3 = 125 colors
            else -> 6              // 6^3 = 216 colors
        }

        val step = 256 / levels

        val r = ((color.red / step) * step).coerceIn(0, 255)
        val g = ((color.green / step) * step).coerceIn(0, 255)
        val b = ((color.blue / step) * step).coerceIn(0, 255)

        return Color(r, g, b)
    }

    /**
     * Draws a single bead in the specified style.
     */
    private fun drawBead(
        g2d: Graphics2D,
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int,
        color: Color,
        options: GridRenderOptions
    ) {
        val width = x2 - x1
        val height = y2 - y1

        // Add a slight highlight for 3D effect
        val highlightColor = Color(
            min(color.red + 40, 255),
            min(color.green + 40, 255),
            min(color.blue + 40, 255)
        )

        when (options.beadStyle) {
            BeadStyle.SQUARE -> {
                g2d.color = color
                g2d.fillRect(x1, y1, width, height)
            }

            BeadStyle.CIRCLE -> {
                // Draw a circular bead with padding
                val padding = (width * 0.1f).toInt()
                g2d.color = color
                g2d.fillOval(x1 + padding, y1 + padding, width - padding * 2, height - padding * 2)

                // Add highlight for 3D effect
                val highlightSize = (width * 0.25f).toInt()
                g2d.color = highlightColor
                g2d.fillOval(
                    x1 + width / 3,
                    y1 + height / 3,
                    highlightSize,
                    highlightSize
                )
            }

            BeadStyle.ROUNDED_SQUARE -> {
                // Draw a rounded rectangle
                val padding = (width * 0.05f).toInt()
                val arc = (width * 0.3f).toInt()
                g2d.color = color
                g2d.fillRoundRect(x1 + padding, y1 + padding, width - padding * 2, height - padding * 2, arc, arc)

                // Add highlight for 3D effect
                val highlightSize = (width * 0.2f).toInt()
                g2d.color = highlightColor
                g2d.fillOval(
                    x1 + width / 3,
                    y1 + height / 3,
                    highlightSize,
                    highlightSize
                )
            }
        }
    }

    /**
     * Draws grid lines over the image.
     */
    private fun drawGridLines(
        g2d: Graphics2D,
        width: Int,
        height: Int,
        gridSize: Int,
        cellWidth: Float,
        cellHeight: Float
    ) {
        g2d.color = Color(0, 0, 0, 128) // Semi-transparent black

        // Draw vertical lines
        for (i in 0..gridSize) {
            val x = (i * cellWidth).toInt()
            g2d.drawLine(x, 0, x, height)
        }

        // Draw horizontal lines
        for (i in 0..gridSize) {
            val y = (i * cellHeight).toInt()
            g2d.drawLine(0, y, width, y)
        }
    }
}

/**
 * Bead rendering style options.
 */
enum class BeadStyle(val displayName: String) {
    SQUARE("Square"),
    CIRCLE("Circle"),
    ROUNDED_SQUARE("Rounded Square")
}

/**
 * Configuration options for grid rendering.
 */
data class GridRenderOptions(
    val enabled: Boolean = false,
    val gridSize: Int = 32,                    // Number of beads per side (32x32 grid)
    val showGridLines: Boolean = true,         // Show grid lines between beads
    val beadStyle: BeadStyle = BeadStyle.ROUNDED_SQUARE,
    val quantizeColors: Boolean = true,        // Reduce colors to palette
    val colorCount: Int = 27                   // Number of distinct colors (3^3)
)
