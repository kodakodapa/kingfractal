package com.github.kodakodapa.kingfractal

import com.github.kodakodapa.kingfractal.gui.KingFractalGUI
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Main entry point for the KingFractal application
 */
fun main(args: Array<String>) {
    println("Starting KingFractal Palette Viewer...")

    // Parse command line arguments
    val options = parseCommandLineArgs(args)

    if (options.showHelp) {
        printUsage()
        return
    }

    if (options.headless) {
        runHeadlessMode(options)
    } else {
        runGuiMode()
    }
}

/**
 * Command line options
 */
data class CommandLineOptions(
    val showHelp: Boolean = false,
    val headless: Boolean = false,
    val outputFile: String? = null,
    val palette: String? = null
)

/**
 * Parse command line arguments
 */
fun parseCommandLineArgs(args: Array<String>): CommandLineOptions {
    var showHelp = false
    var headless = false
    var outputFile: String? = null
    var palette: String? = null

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-h", "--help" -> showHelp = true
            "--headless" -> headless = true
            "-o", "--output" -> {
                if (i + 1 < args.size) {
                    outputFile = args[++i]
                }
            }
            "-p", "--palette" -> {
                if (i + 1 < args.size) {
                    palette = args[++i]
                }
            }
        }
        i++
    }

    return CommandLineOptions(showHelp, headless, outputFile, palette)
}

/**
 * Print usage information
 */
fun printUsage() {
    println("""
        KingFractal Palette Viewer

        Usage: java -jar kingfractal.jar [options]

        Options:
            -h, --help          Show this help message
            --headless          Run in headless mode (no GUI)
            -o, --output FILE   Output file for headless mode
            -p, --palette NAME  Palette name for headless mode

        Examples:
            java -jar kingfractal.jar
                Launch GUI mode

            java -jar kingfractal.jar --headless -o all_palettes.png
                Generate all palettes image in headless mode

            java -jar kingfractal.jar --headless -p "Rainbow" -o rainbow.png
                Generate specific palette image in headless mode
    """.trimIndent())
}

/**
 * Run the application in headless mode (no GUI)
 */
fun runHeadlessMode(options: CommandLineOptions) {
    println("Running in headless mode...")

    try {
        val renderer = com.github.kodakodapa.kingfractal.outputs.PaletteRender()
        val outputFile = options.outputFile ?: "palette_output.png"

        if (options.palette != null) {
            // Render specific palette
            val palette = com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry.getPalette(options.palette)
            if (palette != null) {
                println("Rendering palette: ${options.palette}")
                renderer.saveAsPng(outputFile, palette)
            } else {
                println("Error: Palette '${options.palette}' not found")
                println("Available palettes:")
                com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry.getPaletteNames().forEach {
                    println("  - $it")
                }
                return
            }
        } else {
            // Render all palettes
            println("Rendering all palettes...")
            renderer.saveAllPalettesAsPng(outputFile)
        }

        println("Headless rendering complete.")
    } catch (e: Exception) {
        println("Error in headless mode: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Run the application in GUI mode
 */
fun runGuiMode() {
    println("Launching GUI...")

    // Set system look and feel for better native appearance
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        println("Using system look and feel: ${UIManager.getLookAndFeel().name}")
    } catch (e: Exception) {
        println("Warning: Could not set system look and feel: ${e.message}")
        // Continue with default look and feel
    }

    // Launch GUI on Event Dispatch Thread
    SwingUtilities.invokeLater {
        try {
            val gui = KingFractalGUI()
            gui.isVisible = true
            println("GUI launched successfully")
        } catch (e: Exception) {
            println("Error launching GUI: ${e.message}")
            e.printStackTrace()
        }
    }
}