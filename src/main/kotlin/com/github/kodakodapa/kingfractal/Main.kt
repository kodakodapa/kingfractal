package com.github.kodakodapa.kingfractal

import com.github.kodakodapa.kingfractal.fractal.*
import com.github.kodakodapa.kingfractal.palette.*
import com.github.kodakodapa.kingfractal.output.*

fun main() {
    println("=== KingFractal - GPU Powered Fractal Engine ===")
    println()
    
    // Display available palettes
    val availablePalettes = PaletteRegistry.getAllPalettes()
    println("Available Palettes:")
    availablePalettes.forEach { palette ->
        println("  - ${palette.name}")
    }
    println()
    
    // Create fractal parameters for a small demo
    val params = FractalParams(
        width = 60,
        height = 30,
        centerX = -0.5,
        centerY = 0.0,
        zoom = 1.0,
        maxIterations = 50
    )
    
    // Create renderer
    val mandelbrotRenderer = MandelbrotRenderer()
    val juliaRenderer = JuliaRenderer()
    
    // Demonstrate different palettes with Mandelbrot set
    println("=== Mandelbrot Set with Different Palettes ===")
    println()
    
    val demoPalettes = listOf("Rainbow", "Fire", "Cool Blue")
    
    for (paletteName in demoPalettes) {
        val palette = PaletteRegistry.getPalette(paletteName)
        if (palette != null) {
            println("--- $paletteName Palette ---")
            val fractalData = mandelbrotRenderer.render(params, palette)
            println(FractalOutput.toAscii(fractalData, 60))
            
            // Save to files
            FractalOutput.savePPM(fractalData, "mandelbrot_${paletteName.lowercase().replace(" ", "_")}.ppm")
            FractalOutput.saveAscii(fractalData, "mandelbrot_${paletteName.lowercase().replace(" ", "_")}.txt")
        }
    }
    
    // Demonstrate Julia set with Rainbow palette
    println("=== Julia Set with Rainbow Palette ===")
    val rainbowPalette = PaletteRegistry.getPalette("Rainbow")
    if (rainbowPalette != null) {
        val juliaData = juliaRenderer.render(params, rainbowPalette, Complex(-0.7, 0.27015))
        println(FractalOutput.toAscii(juliaData, 60))
        FractalOutput.savePPM(juliaData, "julia_rainbow.ppm")
        FractalOutput.saveAscii(juliaData, "julia_rainbow.txt")
    }
    
    // Demonstrate custom gradient palette
    println("=== Custom Gradient Palette (Blue to Orange) ===")
    val customPalette = GradientPalette("Blue-Orange", 0x0000FF, 0xFF6600)
    PaletteRegistry.register(customPalette)
    
    val customData = mandelbrotRenderer.render(params, customPalette)
    println(FractalOutput.toAscii(customData, 60))
    FractalOutput.savePPM(customData, "mandelbrot_custom.ppm")
    
    println()
    println("=== Palette Functionality Demonstrated ===")
    println("✓ Multiple predefined palettes (Rainbow, Fire, Cool Blue)")
    println("✓ Custom gradient palettes") 
    println("✓ Palette registry system")
    println("✓ Runtime palette selection")
    println("✓ Both Mandelbrot and Julia set support")
    println("✓ Multiple output formats (ASCII, PPM)")
    println()
    println("Files generated:")
    println("  - mandelbrot_rainbow.ppm/txt")
    println("  - mandelbrot_fire.ppm/txt") 
    println("  - mandelbrot_cool_blue.ppm/txt")
    println("  - julia_rainbow.ppm/txt")
    println("  - mandelbrot_custom.ppm")
}