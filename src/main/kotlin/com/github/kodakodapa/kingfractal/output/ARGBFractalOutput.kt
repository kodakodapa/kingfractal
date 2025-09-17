package com.github.kodakodapa.kingfractal.output

import com.github.kodakodapa.kingfractal.color.ARGBColor
import com.github.kodakodapa.kingfractal.color.ARGBColorMatrix
import com.github.kodakodapa.kingfractal.fractal.ARGBFractalResult
import java.io.File

/**
 * Output utilities for ARGB fractal data with full alpha support
 */
object ARGBFractalOutput {
    
    /**
     * Convert ARGB color to ANSI escape code for terminal display
     * Handles transparency by blending with background
     */
    private fun argbToAnsi(color: ARGBColor, backgroundColor: ARGBColor = ARGBColor.BLACK): String {
        // Blend with background if transparent
        val blended = if (color.alpha < 255) {
            val alpha = color.alpha / 255f
            val invAlpha = 1f - alpha
            ARGBColor(
                255,
                (color.red * alpha + backgroundColor.red * invAlpha).toInt().coerceIn(0, 255),
                (color.green * alpha + backgroundColor.green * invAlpha).toInt().coerceIn(0, 255),
                (color.blue * alpha + backgroundColor.blue * invAlpha).toInt().coerceIn(0, 255)
            )
        } else {
            color
        }
        
        return "\u001B[38;2;${blended.red};${blended.green};${blended.blue}m"
    }
    
    /**
     * Display ARGB fractal data in the terminal using ANSI colors
     */
    fun displayInTerminal(result: ARGBFractalResult, char: String = "█") {
        val reset = "\u001B[0m"
        
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                val color = result.getPixel(x, y)
                if (color == ARGBColor.BLACK) {
                    print(" ") // Black pixels as spaces
                } else {
                    print("${argbToAnsi(color)}${char}${reset}")
                }
            }
            println()
        }
    }
    
    /**
     * Save ARGB fractal data as a PPM image file (RGB only, transparency is blended)
     */
    fun savePPM(result: ARGBFractalResult, filename: String, backgroundColor: ARGBColor = ARGBColor.WHITE) {
        val file = File(filename)
        file.writeText(buildString {
            appendLine("P3")
            appendLine("${result.width} ${result.height}")
            appendLine("255")
            
            for (y in 0 until result.height) {
                for (x in 0 until result.width) {
                    val color = result.getPixel(x, y)
                    // Blend with background if transparent
                    val blended = if (color.alpha < 255) {
                        val alpha = color.alpha / 255f
                        val invAlpha = 1f - alpha
                        ARGBColor(
                            255,
                            (color.red * alpha + backgroundColor.red * invAlpha).toInt().coerceIn(0, 255),
                            (color.green * alpha + backgroundColor.green * invAlpha).toInt().coerceIn(0, 255),
                            (color.blue * alpha + backgroundColor.blue * invAlpha).toInt().coerceIn(0, 255)
                        )
                    } else {
                        color
                    }
                    append("${blended.red} ${blended.green} ${blended.blue} ")
                }
                appendLine()
            }
        })
        println("Saved ARGB fractal to $filename")
    }
    
    /**
     * Save as PNG with full alpha support (requires additional dependencies for actual PNG writing)
     * For now, saves metadata about alpha channel
     */
    fun savePNGMeta(result: ARGBFractalResult, filename: String) {
        val metaFile = File("${filename}.meta")
        metaFile.writeText(buildString {
            appendLine("# ARGB Fractal Metadata")
            appendLine("width=${result.width}")
            appendLine("height=${result.height}")
            appendLine("has_transparency=${hasTransparency(result)}")
            appendLine("# Format: x,y,A,R,G,B")
            
            for (y in 0 until result.height) {
                for (x in 0 until result.width) {
                    val vector = result.getPixelVector(x, y)
                    appendLine("$x,$y,${vector[0]},${vector[1]},${vector[2]},${vector[3]}")
                }
            }
        })
        println("Saved ARGB metadata to ${filename}.meta")
    }
    
    /**
     * Save the [255][4] vector matrix to file
     * This provides the requested [255][4] size vectors functionality
     */
    fun saveVectorMatrix(result: ARGBFractalResult, filename: String) {
        val file = File(filename)
        file.writeText(buildString {
            appendLine("# ARGB Vector Matrix [${result.height}][${result.width}][4]")
            appendLine("# Format: row,col,[A,R,G,B]")
            
            val vectorMatrix = result.getVectorMatrix()
            for (y in vectorMatrix.indices) {
                for (x in vectorMatrix[y].indices) {
                    val vector = vectorMatrix[y][x]
                    appendLine("$y,$x,[${vector[0]},${vector[1]},${vector[2]},${vector[3]}]")
                }
            }
        })
        println("Saved vector matrix to $filename")
    }
    
    /**
     * Save a palette's [255][4] color matrix
     */
    fun savePaletteMatrix(matrix: ARGBColorMatrix, filename: String) {
        val file = File(filename)
        file.writeText(buildString {
            appendLine("# Palette Color Matrix [255][4]")
            appendLine("# Format: index,[A,R,G,B]")
            
            for (i in 0 until matrix.size) {
                val vector = matrix.getVector(i)
                appendLine("$i,[${vector[0]},${vector[1]},${vector[2]},${vector[3]}]")
            }
        })
        println("Saved palette matrix to $filename")
    }
    
    /**
     * Generate ASCII art representation of the ARGB fractal
     */
    fun toAscii(result: ARGBFractalResult, width: Int = 80, backgroundColor: ARGBColor = ARGBColor.BLACK): String {
        val chars = " .:-=+*#%@"
        val stringResult = StringBuilder()
        val scaleX = result.width.toDouble() / width
        val scaleY = result.height.toDouble() / (width / 2) // Adjust for character aspect ratio
        
        for (y in 0 until (width / 2)) {
            for (x in 0 until width) {
                val sourceX = (x * scaleX).toInt().coerceIn(0, result.width - 1)
                val sourceY = (y * scaleY).toInt().coerceIn(0, result.height - 1)
                val color = result.getPixel(sourceX, sourceY)
                
                if (color == ARGBColor.BLACK) {
                    stringResult.append(' ')
                } else {
                    // Blend with background if transparent
                    val blended = if (color.alpha < 255) {
                        val alpha = color.alpha / 255f
                        val invAlpha = 1f - alpha
                        ARGBColor(
                            255,
                            (color.red * alpha + backgroundColor.red * invAlpha).toInt().coerceIn(0, 255),
                            (color.green * alpha + backgroundColor.green * invAlpha).toInt().coerceIn(0, 255),
                            (color.blue * alpha + backgroundColor.blue * invAlpha).toInt().coerceIn(0, 255)
                        )
                    } else {
                        color
                    }
                    
                    val brightness = blended.red + blended.green + blended.blue
                    val charIndex = (brightness * chars.length / (255 * 3)).coerceIn(0, chars.length - 1)
                    stringResult.append(chars[charIndex])
                }
            }
            stringResult.appendLine()
        }
        
        return stringResult.toString()
    }
    
    /**
     * Save fractal as ASCII art to a file
     */
    fun saveAscii(result: ARGBFractalResult, filename: String, width: Int = 80) {
        File(filename).writeText(toAscii(result, width))
        println("Saved ASCII art to $filename")
    }
    
    /**
     * Check if the fractal result contains transparency
     */
    fun hasTransparency(result: ARGBFractalResult): Boolean {
        for (y in 0 until result.height) {
            for (x in 0 until result.width) {
                if (result.getPixel(x, y).alpha < 255) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Generate transparency map as ASCII where different characters represent alpha levels
     */
    fun generateTransparencyMap(result: ARGBFractalResult, width: Int = 80): String {
        val alphaChars = " ░▒▓█" // Different levels of transparency
        val stringResult = StringBuilder()
        val scaleX = result.width.toDouble() / width
        val scaleY = result.height.toDouble() / (width / 2)
        
        for (y in 0 until (width / 2)) {
            for (x in 0 until width) {
                val sourceX = (x * scaleX).toInt().coerceIn(0, result.width - 1)
                val sourceY = (y * scaleY).toInt().coerceIn(0, result.height - 1)
                val alpha = result.getPixel(sourceX, sourceY).alpha
                
                val charIndex = (alpha * alphaChars.length / 256).coerceIn(0, alphaChars.length - 1)
                stringResult.append(alphaChars[charIndex])
            }
            stringResult.appendLine()
        }
        
        return stringResult.toString()
    }
    
    /**
     * Save transparency map to file
     */
    fun saveTransparencyMap(result: ARGBFractalResult, filename: String, width: Int = 80) {
        File(filename).writeText(generateTransparencyMap(result, width))
        println("Saved transparency map to $filename")
    }
}