package com.github.kodakodapa.kingfractal.output

import java.io.File
import java.io.FileWriter

/**
 * Simple utilities for outputting fractal data
 */
object FractalOutput {
    
    /**
     * Convert RGB color to ANSI escape code for terminal display
     */
    private fun rgbToAnsi(rgb: Int): String {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return "\u001B[38;2;${r};${g};${b}m"
    }
    
    /**
     * Display fractal data in the terminal using ANSI colors
     */
    fun displayInTerminal(data: Array<IntArray>, char: String = "█") {
        val reset = "\u001B[0m"
        
        for (row in data) {
            for (pixel in row) {
                if (pixel == 0) {
                    print(" ") // Black pixels as spaces
                } else {
                    print("${rgbToAnsi(pixel)}${char}${reset}")
                }
            }
            println()
        }
    }
    
    /**
     * Save fractal data as a PPM image file
     */
    fun savePPM(data: Array<IntArray>, filename: String) {
        val file = File(filename)
        file.writeText(buildString {
            appendLine("P3")
            appendLine("${data[0].size} ${data.size}")
            appendLine("255")
            
            for (row in data) {
                for (pixel in row) {
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    append("$r $g $b ")
                }
                appendLine()
            }
        })
        println("Saved fractal to $filename")
    }
    
    /**
     * Generate ASCII art representation of the fractal
     */
    fun toAscii(data: Array<IntArray>, width: Int = 80): String {
        val chars = " .:-=+*#%@"
        val result = StringBuilder()
        val scaleX = data[0].size.toDouble() / width
        val scaleY = data.size.toDouble() / (width / 2) // Adjust for character aspect ratio
        
        for (y in 0 until (width / 2)) {
            for (x in 0 until width) {
                val sourceX = (x * scaleX).toInt().coerceIn(0, data[0].size - 1)
                val sourceY = (y * scaleY).toInt().coerceIn(0, data.size - 1)
                val pixel = data[sourceY][sourceX]
                
                if (pixel == 0) {
                    result.append(' ')
                } else {
                    val brightness = ((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)
                    val charIndex = (brightness * chars.length / (255 * 3)).coerceIn(0, chars.length - 1)
                    result.append(chars[charIndex])
                }
            }
            result.appendLine()
        }
        
        return result.toString()
    }
    
    /**
     * Save fractal as ASCII art to a file
     */
    fun saveAscii(data: Array<IntArray>, filename: String, width: Int = 80) {
        File(filename).writeText(toAscii(data, width))
        println("Saved ASCII art to $filename")
    }
}