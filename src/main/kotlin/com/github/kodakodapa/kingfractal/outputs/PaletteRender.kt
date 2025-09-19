package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.ARGBColor
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
    fun toGradientImage(palette: ARGBPalette?, maxIterations: Int = 255): BufferedImage {
        // Handle null palette
        val safePalette = palette ?: ARGBRainbowPalette()
        val safeMaxIterations = maxIterations.coerceAtLeast(1)

        val image = BufferedImage(GRADIENT_WIDTH, GRADIENT_HEIGHT * 3, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            // Row 1: RGB only (ignoring alpha)
            for (x in 0 until GRADIENT_WIDTH) {
                val iteration = (x * safeMaxIterations / GRADIENT_WIDTH.toFloat()).toInt().coerceIn(0, safeMaxIterations - 1)

                val color = try {
                    safePalette.getColor(iteration, safeMaxIterations)
                } catch (_: Exception) {
                    ARGBColor.BLACK // Fallback color
                }

                val rgbColor = Color(color.red, color.green, color.blue, 255)
                g2d.color = rgbColor
                g2d.fillRect(x, 0, 1, GRADIENT_HEIGHT)
            }

            // Row 2: Alpha channel as grayscale
            for (x in 0 until GRADIENT_WIDTH) {
                val iteration = (x * safeMaxIterations / GRADIENT_WIDTH.toFloat()).toInt().coerceIn(0, safeMaxIterations - 1)

                val color = try {
                    safePalette.getColor(iteration, safeMaxIterations)
                } catch (_: Exception) {
                    ARGBColor.BLACK
                }

                val gray = color.alpha
                val grayColor = Color(gray, gray, gray, 255)
                g2d.color = grayColor
                g2d.fillRect(x, GRADIENT_HEIGHT, 1, GRADIENT_HEIGHT)
            }

            // Row 3: Full ARGB with checkerboard background for alpha
            drawCheckerboard(g2d, 0, GRADIENT_HEIGHT * 2, GRADIENT_WIDTH, GRADIENT_HEIGHT)
            for (x in 0 until GRADIENT_WIDTH) {
                val iteration = (x * safeMaxIterations / GRADIENT_WIDTH.toFloat()).toInt().coerceIn(0, safeMaxIterations - 1)

                val color = try {
                    safePalette.getColor(iteration, safeMaxIterations)
                } catch (_: Exception) {
                    ARGBColor.BLACK
                }

                val argbColor = Color(color.red, color.green, color.blue, color.alpha)
                g2d.color = argbColor
                g2d.fillRect(x, GRADIENT_HEIGHT * 2, 1, GRADIENT_HEIGHT)
            }
        } catch (_: Exception) {
            // If everything fails, fill with a simple gradient
            g2d.color = Color.RED
            g2d.fillRect(0, 0, GRADIENT_WIDTH, GRADIENT_HEIGHT)
            g2d.color = Color.GREEN
            g2d.fillRect(0, GRADIENT_HEIGHT, GRADIENT_WIDTH, GRADIENT_HEIGHT)
            g2d.color = Color.BLUE
            g2d.fillRect(0, GRADIENT_HEIGHT * 2, GRADIENT_WIDTH, GRADIENT_HEIGHT)
        } finally {
            g2d.dispose()
        }

        return image
    }

    /**
     * Creates a swatch grid visualization showing individual colors from the palette
     */
    fun toSwatchGrid(palette: ARGBPalette?, maxIterations: Int = 255): BufferedImage {
        // Handle null palette and invalid parameters
        val safePalette = palette ?: ARGBRainbowPalette()
        val safeMaxIterations = maxIterations.coerceAtLeast(1)

        val cols = 16
        val rows = 16
        val imageWidth = cols * SWATCH_SIZE * 2
        val imageHeight = rows * SWATCH_SIZE

        val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val totalSwatches = rows * cols
                    val iteration =
                        ((row * cols + col) * safeMaxIterations / totalSwatches).coerceIn(0, safeMaxIterations - 1)

                    val color = try {
                        safePalette.getColor(iteration, safeMaxIterations)
                    } catch (_: Exception) {
                        ARGBColor.BLACK // Fallback color
                    }

                    // Left half: RGB only
                    val rgbColor = Color(color.red, color.green, color.blue, 255)
                    g2d.color = rgbColor
                    g2d.fillRect(col * SWATCH_SIZE * 2, row * SWATCH_SIZE, SWATCH_SIZE, SWATCH_SIZE)

                    // Right half: ARGB with checkerboard
                    try {
                        drawCheckerboard(g2d, col * SWATCH_SIZE * 2 + SWATCH_SIZE, row * SWATCH_SIZE,
                                        SWATCH_SIZE, SWATCH_SIZE)
                        val argbColor = Color(color.red, color.green, color.blue, color.alpha)
                        g2d.color = argbColor
                        g2d.fillRect(col * SWATCH_SIZE * 2 + SWATCH_SIZE, row * SWATCH_SIZE,
                                    SWATCH_SIZE, SWATCH_SIZE)
                    } catch (_: Exception) {
                        // If checkerboard fails, just fill with solid color
                        g2d.color = Color.GRAY
                        g2d.fillRect(col * SWATCH_SIZE * 2 + SWATCH_SIZE, row * SWATCH_SIZE,
                                    SWATCH_SIZE, SWATCH_SIZE)
                    }
                }
            }
        } catch (_: Exception) {
            // If everything fails, fill with a simple pattern
            g2d.color = Color.LIGHT_GRAY
            g2d.fillRect(0, 0, imageWidth, imageHeight)
        } finally {
            g2d.dispose()
        }

        return image
    }

    /**
     * Creates a comprehensive visualization with multiple views of the palette
     */
    fun toComprehensiveImage(palette: ARGBPalette?, maxIterations: Int = 255): BufferedImage {
        // Handle null palette
        val safePalette = palette ?: ARGBRainbowPalette()
        val safeMaxIterations = maxIterations.coerceAtLeast(1)

        val gradientImg = toGradientImage(safePalette, safeMaxIterations)
        val swatchImg = toSwatchGrid(safePalette, safeMaxIterations)

        val totalHeight = gradientImg.height + swatchImg.height + 40
        val totalWidth = kotlin.math.max(gradientImg.width, swatchImg.width)

        val image = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()

        try {
            // White background
            g2d.color = Color.WHITE
            g2d.fillRect(0, 0, totalWidth, totalHeight)

            // Draw palette name
            g2d.color = Color.BLACK
            val paletteName = safePalette.name
            g2d.drawString(paletteName, 10, 20)

            // Draw gradient visualization
            g2d.drawImage(gradientImg, 0, 30, null)

            // Add labels for gradient rows
            g2d.drawString("RGB Colors", 10, 30 + GRADIENT_HEIGHT / 2)
            g2d.drawString("Alpha Channel", 10, 30 + GRADIENT_HEIGHT + GRADIENT_HEIGHT / 2)
            g2d.drawString("ARGB Combined", 10, 30 + GRADIENT_HEIGHT * 2 + GRADIENT_HEIGHT / 2)

            // Draw swatch grid
            g2d.drawImage(swatchImg, 0, 30 + gradientImg.height + 10, null)
        } catch (_: Exception) {
            // If composition fails, create a simple error image
            g2d.color = Color.RED
            g2d.fillRect(0, 0, totalWidth, totalHeight)
            g2d.color = Color.WHITE
            g2d.drawString("Error rendering palette", 10, 20)
        } finally {
            g2d.dispose()
        }

        return image
    }

    /**
     * Renders all palettes from the registry into a single large image
     */
    fun renderAllPalettes(): BufferedImage {
        return try {
            val palettes = ARGBPaletteRegistry.getAllPalettes()

            if (palettes.isEmpty()) {
                // Create a simple error image
                val errorImage = BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB)
                val g2d = errorImage.createGraphics()
                g2d.color = Color.LIGHT_GRAY
                g2d.fillRect(0, 0, 400, 100)
                g2d.color = Color.BLACK
                g2d.drawString("No palettes found in registry", 10, 50)
                g2d.dispose()
                return errorImage
            }

            val paletteImages = palettes.mapNotNull { palette ->
                try {
                    toComprehensiveImage(palette)
                } catch (_: Exception) {
                    null // Skip palettes that fail to render
                }
            }

            if (paletteImages.isEmpty()) {
                // All palettes failed to render
                val errorImage = BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB)
                val g2d = errorImage.createGraphics()
                g2d.color = Color.RED
                g2d.fillRect(0, 0, 400, 100)
                g2d.color = Color.WHITE
                g2d.drawString("All palettes failed to render", 10, 50)
                g2d.dispose()
                return errorImage
            }

            val maxWidth = paletteImages.maxOfOrNull { it.width } ?: 400
            val totalHeight = paletteImages.sumOf { it.height } + (paletteImages.size - 1) * 20

            val image = BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB)
            val g2d = image.createGraphics()

            try {
                // Light gray background
                g2d.color = Color(240, 240, 240)
                g2d.fillRect(0, 0, maxWidth, totalHeight)

                var currentY = 0
                for (paletteImage in paletteImages) {
                    g2d.drawImage(paletteImage, 0, currentY, null)
                    currentY += paletteImage.height + 20
                }
            } finally {
                g2d.dispose()
            }

            image
        } catch (e: Exception) {
            // Ultimate fallback - create a simple error image
            val errorImage = BufferedImage(400, 100, BufferedImage.TYPE_INT_ARGB)
            val g2d = errorImage.createGraphics()
            g2d.color = Color.RED
            g2d.fillRect(0, 0, 400, 100)
            g2d.color = Color.WHITE
            g2d.drawString("Critical error in palette rendering", 10, 30)
            g2d.drawString(e.message ?: "Unknown error", 10, 60)
            g2d.dispose()
            errorImage
        }
    }

    /**
     * Draws a checkerboard pattern (useful for visualizing transparency)
     */
    private fun drawCheckerboard(g2d: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        // Validate input parameters
        if (width <= 0 || height <= 0) return

        val light = Color(255, 255, 255)
        val dark = Color(200, 200, 200)

        val cols = ceil(width.toDouble() / CHECKERBOARD_SIZE).toInt().coerceAtLeast(1)
        val rows = ceil(height.toDouble() / CHECKERBOARD_SIZE).toInt().coerceAtLeast(1)

        try {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    g2d.color = if ((row + col) % 2 == 0) light else dark
                    val rectX = x + col * CHECKERBOARD_SIZE
                    val rectY = y + row * CHECKERBOARD_SIZE
                    val rectW = kotlin.math.min(CHECKERBOARD_SIZE, x + width - rectX).coerceAtLeast(0)
                    val rectH = kotlin.math.min(CHECKERBOARD_SIZE, y + height - rectY).coerceAtLeast(0)

                    if (rectW > 0 && rectH > 0) {
                        g2d.fillRect(rectX, rectY, rectW, rectH)
                    }
                }
            }
        } catch (_: Exception) {
            // If checkerboard fails, just fill with solid light color
            g2d.color = light
            g2d.fillRect(x, y, width, height)
        }
    }

    /**
     * Saves a palette visualization as PNG
     */
    fun saveAsPng(filename: String, palette: ARGBPalette?) {
        if (filename.isBlank()) {
            println("Error: Invalid filename provided")
            return
        }

        try {
            val bufferedImage = toComprehensiveImage(palette)
            val file = File(filename)

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            val success = ImageIO.write(bufferedImage, "PNG", file)
            if (success && file.exists()) {
                println("Saved PNG: $filename (${file.length()} bytes)")
            } else {
                println("Warning: PNG save may have failed for $filename")
            }
        } catch (e: Exception) {
            println("Error saving PNG '$filename': ${e.message}")
        }
    }

    /**
     * Saves all palettes from registry as a single PNG
     */
    fun saveAllPalettesAsPng(filename: String) {
        if (filename.isBlank()) {
            println("Error: Invalid filename provided")
            return
        }

        try {
            val bufferedImage = renderAllPalettes()
            val file = File(filename)

            // Ensure parent directory exists
            file.parentFile?.mkdirs()

            val success = ImageIO.write(bufferedImage, "PNG", file)
            if (success && file.exists()) {
                println("Saved all palettes PNG: $filename (${file.length()} bytes)")
            } else {
                println("Warning: PNG save may have failed for $filename")
            }
        } catch (e: Exception) {
            println("Error saving all palettes PNG '$filename': ${e.message}")
        }
    }
}
