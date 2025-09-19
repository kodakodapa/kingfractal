package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.ARGBPalette
import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry
import com.github.kodakodapa.kingfractal.colors.ARGBRainbowPalette
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil


class PaletteRender {

    companion object {
        const val GRADIENT_HEIGHT = 50
        const val GRADIENT_WIDTH = 512
        const val SWATCH_SIZE = 16
        const val CHECKERBOARD_SIZE = 8
    }

    /**
     * Creates a gradient visualization of a palette
     * Shows the full palette gradient with alpha visualization
     */
    fun toGradientImage(palette: ARGBPalette, maxIterations: Int = 255): BufferedImage {
        val image = BufferedImage(GRADIENT_WIDTH, GRADIENT_HEIGHT * 3, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // Row 1: RGB only (ignoring alpha)
        for (x in 0 until GRADIENT_WIDTH) {
            val iteration = (x * maxIterations / GRADIENT_WIDTH.toFloat()).toInt()
            val color = palette.getColor(iteration, maxIterations)
            val rgbColor = Color(color.red, color.green, color.blue, 255)
            g2d.color = rgbColor
            g2d.fillRect(x, 0, 1, GRADIENT_HEIGHT)
        }

        // Row 2: Alpha channel as grayscale
        for (x in 0 until GRADIENT_WIDTH) {
            val iteration = (x * maxIterations / GRADIENT_WIDTH.toFloat()).toInt()
            val color = palette.getColor(iteration, maxIterations)
            val gray = color.alpha
            val grayColor = Color(gray, gray, gray, 255)
            g2d.color = grayColor
            g2d.fillRect(x, GRADIENT_HEIGHT, 1, GRADIENT_HEIGHT)
        }

        // Row 3: Full ARGB with checkerboard background for alpha
        drawCheckerboard(g2d, 0, GRADIENT_HEIGHT * 2, GRADIENT_WIDTH, GRADIENT_HEIGHT)
        for (x in 0 until GRADIENT_WIDTH) {
            val iteration = (x * maxIterations / GRADIENT_WIDTH.toFloat()).toInt()
            val color = palette.getColor(iteration, maxIterations)
            val argbColor = Color(color.red, color.green, color.blue, color.alpha)
            g2d.color = argbColor
            g2d.fillRect(x, GRADIENT_HEIGHT * 2, 1, GRADIENT_HEIGHT)
        }

        g2d.dispose()
        return image
    }

    /**
     * Creates a swatch grid visualization showing individual colors from the palette
     */
    fun toSwatchGrid(palette: ARGBPalette, maxIterations: Int = 255): BufferedImage {
        val cols = 16
        val rows = 16
        val imageWidth = cols * SWATCH_SIZE * 2
        val imageHeight = rows * SWATCH_SIZE

        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val iteration = (row * cols + col) * maxIterations / (rows * cols)
                val color = palette.getColor(iteration, maxIterations)

                // Left half: RGB only
                val rgbColor = Color(color.red, color.green, color.blue, 255)
                g2d.color = rgbColor
                g2d.fillRect(col * SWATCH_SIZE * 2, row * SWATCH_SIZE, SWATCH_SIZE, SWATCH_SIZE)

                // Right half: ARGB with checkerboard
                drawCheckerboard(g2d, col * SWATCH_SIZE * 2 + SWATCH_SIZE, row * SWATCH_SIZE,
                                SWATCH_SIZE, SWATCH_SIZE)
                val argbColor = Color(color.red, color.green, color.blue, color.alpha)
                g2d.color = argbColor
                g2d.fillRect(col * SWATCH_SIZE * 2 + SWATCH_SIZE, row * SWATCH_SIZE,
                            SWATCH_SIZE, SWATCH_SIZE)
            }
        }

        g2d.dispose()
        return image
    }

    /**
     * Creates a comprehensive visualization with multiple views of the palette
     */
    fun toComprehensiveImage(palette: ARGBPalette, maxIterations: Int = 255): BufferedImage {
        val gradientImg = toGradientImage(palette, maxIterations)
        val swatchImg = toSwatchGrid(palette, maxIterations)

        val totalHeight = gradientImg.height + swatchImg.height + 40
        val totalWidth = kotlin.math.max(gradientImg.width, swatchImg.width)

        val image = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // White background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, totalWidth, totalHeight)

        // Draw palette name
        g2d.color = Color.BLACK
        g2d.drawString(palette.name, 10, 20)

        // Draw gradient visualization
        g2d.drawImage(gradientImg, 0, 30, null)

        // Add labels for gradient rows
        g2d.drawString("RGB Colors", 10, 30 + GRADIENT_HEIGHT / 2)
        g2d.drawString("Alpha Channel", 10, 30 + GRADIENT_HEIGHT + GRADIENT_HEIGHT / 2)
        g2d.drawString("ARGB Combined", 10, 30 + GRADIENT_HEIGHT * 2 + GRADIENT_HEIGHT / 2)

        // Draw swatch grid
        g2d.drawImage(swatchImg, 0, 30 + gradientImg.height + 10, null)

        g2d.dispose()
        return image
    }

    /**
     * Renders all palettes from the registry into a single large image
     */
    fun renderAllPalettes(): BufferedImage {
        val palettes = ARGBPaletteRegistry.getAllPalettes()
        val paletteImages = palettes.map { palette ->
            toComprehensiveImage(palette)
        }

        if (paletteImages.isEmpty()) {
            return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }

        val maxWidth = paletteImages.maxOf { it.width }
        val totalHeight = paletteImages.sumOf { it.height } + (palettes.size - 1) * 20

        val image = BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        // Light gray background
        g2d.color = Color(240, 240, 240)
        g2d.fillRect(0, 0, maxWidth, totalHeight)

        var currentY = 0
        for (paletteImage in paletteImages) {
            g2d.drawImage(paletteImage, 0, currentY, null)
            currentY += paletteImage.height + 20
        }

        g2d.dispose()
        return image
    }

    /**
     * Draws a checkerboard pattern (useful for visualizing transparency)
     */
    private fun drawCheckerboard(g2d: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        val light = Color(255, 255, 255)
        val dark = Color(200, 200, 200)

        val cols = ceil(width.toDouble() / CHECKERBOARD_SIZE).toInt()
        val rows = ceil(height.toDouble() / CHECKERBOARD_SIZE).toInt()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                g2d.color = if ((row + col) % 2 == 0) light else dark
                val rectX = x + col * CHECKERBOARD_SIZE
                val rectY = y + row * CHECKERBOARD_SIZE
                val rectW = kotlin.math.min(CHECKERBOARD_SIZE, x + width - rectX)
                val rectH = kotlin.math.min(CHECKERBOARD_SIZE, y + height - rectY)
                g2d.fillRect(rectX, rectY, rectW, rectH)
            }
        }
    }

    /**
     * Saves a palette visualization as PNG
     */
    fun saveAsPng(filename: String, palette: ARGBPalette) {
        try {
            val bufferedImage = toComprehensiveImage(palette)
            ImageIO.write(bufferedImage, "PNG", File(filename))
            println("Saved PNG: $filename (${File(filename).length()} bytes)")
        } catch (e: Exception) {
            println("Error saving PNG: ${e.message}")
        }
    }

    /**
     * Saves all palettes from registry as a single PNG
     */
    fun saveAllPalettesAsPng(filename: String) {
        try {
            val bufferedImage = renderAllPalettes()
            ImageIO.write(bufferedImage, "PNG", File(filename))
            println("Saved all palettes PNG: $filename (${File(filename).length()} bytes)")
        } catch (e: Exception) {
            println("Error saving PNG: ${e.message}")
        }
    }
}
