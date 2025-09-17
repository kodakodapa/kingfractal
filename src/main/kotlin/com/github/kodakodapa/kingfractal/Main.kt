package org.example.com.github.kodakodapa.kingfractal

import com.github.kodakodapa.kingfractal.color.*
import com.github.kodakodapa.kingfractal.fractal.*
import com.github.kodakodapa.kingfractal.palette.*
import com.github.kodakodapa.kingfractal.output.*

fun main() {
    println("=== KingFractal - GPU Powered Fractal Engine with Full ARGB Support ===")
    println()
    
    // Display available ARGB palettes
    val availablePalettes = ARGBPaletteRegistry.getAllPalettes()
    println("Available ARGB Palettes (${availablePalettes.size} total):")
    availablePalettes.forEach { palette ->
        val transparencyStatus = if (palette.supportsTransparency) " [TRANSPARENT]" else " [OPAQUE]"
        println("  - ${palette.name}$transparencyStatus")
    }
    println()
    
    // Demonstrate [255][4] color matrix functionality
    println("=== [255][4] Color Matrix Demo ===")
    val rainbowPalette = ARGBPaletteRegistry.getPalette("Rainbow")!!
    val colorMatrix = rainbowPalette.generateColorMatrix(255)
    
    println("Generated [255][4] color matrix for Rainbow palette")
    println("Sample vectors from matrix:")
    for (i in 0..4) {
        val vector = colorMatrix.getVector(i * 50)
        println("  Index ${i * 50}: [A=${vector[0]}, R=${vector[1]}, G=${vector[2]}, B=${vector[3]}]")
    }
    
    // Save the color matrix
    ARGBFractalOutput.savePaletteMatrix(colorMatrix, "rainbow_palette_matrix.txt")
    println()
    
    // Create fractal parameters for demos
    val params = FractalParams(
        width = 60,
        height = 30,
        centerX = -0.5,
        centerY = 0.0,
        zoom = 1.0,
        maxIterations = 50
    )
    
    // Create renderers
    val mandelbrotRenderer = ARGBMandelbrotRenderer()
    val juliaRenderer = ARGBJuliaRenderer()
    
    // Demonstrate opaque palettes
    println("=== Mandelbrot Set with Opaque ARGB Palettes ===")
    val opaquePalettes = ARGBPaletteRegistry.getOpaquePalettes().take(3)
    
    for (palette in opaquePalettes) {
        println("--- ${palette.name} Palette ---")
        val fractalResult = mandelbrotRenderer.render(params, palette)
        println(ARGBFractalOutput.toAscii(fractalResult, 60))
        
        // Save in multiple formats
        ARGBFractalOutput.savePPM(fractalResult, "mandelbrot_${palette.name.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}.ppm")
        ARGBFractalOutput.saveVectorMatrix(fractalResult, "mandelbrot_${palette.name.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}_vectors.txt")
        println()
    }
    
    // Demonstrate transparent palettes
    println("=== Transparent ARGB Palettes Demo ===")
    val transparentPalettes = ARGBPaletteRegistry.getTransparentPalettes().take(2)
    
    for (palette in transparentPalettes) {
        println("--- ${palette.name} Palette (with transparency) ---")
        val fractalResult = mandelbrotRenderer.render(params, palette)
        
        // Show regular ASCII
        println("Regular view:")
        println(ARGBFractalOutput.toAscii(fractalResult, 60))
        
        // Show transparency map
        println("Transparency map:")
        println(ARGBFractalOutput.generateTransparencyMap(fractalResult, 60))
        
        // Save with transparency information
        ARGBFractalOutput.savePNGMeta(fractalResult, "mandelbrot_${palette.name.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}.png")
        ARGBFractalOutput.saveTransparencyMap(fractalResult, "mandelbrot_${palette.name.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}_transparency.txt")
        println()
    }
    
    // Demonstrate Julia set with transparency
    println("=== Julia Set with Fire (with Smoke) Palette ===")
    val smokePalette = ARGBPaletteRegistry.getPalette("Fire (with Smoke)")!!
    val juliaResult = juliaRenderer.render(params, smokePalette, Complex(-0.7, 0.27015))
    
    println("Julia set with smoke effects:")
    println(ARGBFractalOutput.toAscii(juliaResult, 60))
    println("Transparency map:")
    println(ARGBFractalOutput.generateTransparencyMap(juliaResult, 60))
    
    ARGBFractalOutput.savePPM(juliaResult, "julia_smoke.ppm")
    ARGBFractalOutput.saveVectorMatrix(juliaResult, "julia_smoke_vectors.txt")
    println()
    
    // Demonstrate custom ARGB gradient with transparency
    println("=== Custom ARGB Gradient with Transparency ===")
    val customPalette = ARGBGradientPalette(
        "Blue to Transparent Red", 
        ARGBColor(255, 0, 0, 255),  // Solid blue
        ARGBColor(100, 255, 0, 0)   // Semi-transparent red
    )
    ARGBPaletteRegistry.register(customPalette)
    
    val customResult = mandelbrotRenderer.render(params, customPalette)
    println("Custom gradient (Blue to Semi-transparent Red):")
    println(ARGBFractalOutput.toAscii(customResult, 60))
    println("Transparency map:")
    println(ARGBFractalOutput.generateTransparencyMap(customResult, 60))
    
    ARGBFractalOutput.savePPM(customResult, "mandelbrot_custom_argb.ppm")
    ARGBFractalOutput.saveVectorMatrix(customResult, "mandelbrot_custom_argb_vectors.txt")
    println()
    
    // Demonstrate multi-layered palette
    println("=== Multi-Layered ARGB Palette ===")
    val basePalette = ARGBPaletteRegistry.getPalette("Rainbow")!!
    val overlayPalette = ARGBPaletteRegistry.getPalette("Fire (with Smoke)")!!
    
    val layeredPalette = ARGBLayeredPalette(
        "Rainbow + Fire Overlay",
        listOf(
            basePalette to 0.7f,    // 70% base layer
            overlayPalette to 0.3f  // 30% overlay
        )
    )
    ARGBPaletteRegistry.register(layeredPalette)
    
    val layeredResult = mandelbrotRenderer.render(params, layeredPalette)
    println("Multi-layered palette (Rainbow base + Fire overlay):")
    println(ARGBFractalOutput.toAscii(layeredResult, 60))
    
    ARGBFractalOutput.savePPM(layeredResult, "mandelbrot_layered.ppm")
    ARGBFractalOutput.saveVectorMatrix(layeredResult, "mandelbrot_layered_vectors.txt")
    println()
    
    // Generate and save all palette matrices
    println("=== Generating All [255][4] Palette Matrices ===")
    val allMatrices = ARGBPaletteRegistry.generateAllColorMatrices(255)
    
    println("Generated [255][4] matrices for ${allMatrices.size} palettes:")
    allMatrices.forEach { (name, matrix) ->
        val filename = "palette_${name.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}_matrix.txt"
        ARGBFractalOutput.savePaletteMatrix(matrix, filename)
        println("  - $name -> $filename")
    }
    println()
    
    println("=== ARGB Functionality Demonstrated ===")
    println("✓ Full ARGB color support with 255-level precision per channel")
    println("✓ [255][4] color vector matrices for all palettes")
    println("✓ Transparency and alpha channel support")
    println("✓ Multiple palette types: opaque, transparent, gradient, layered")
    println("✓ ARGB color interpolation (linear and HSV)")
    println("✓ Multi-format output: PPM, vector matrices, transparency maps")
    println("✓ Both Mandelbrot and Julia set rendering with ARGB")
    println("✓ Custom ARGB palette creation")
    println("✓ Multi-layered palette compositing")
    println("✓ Comprehensive [255][4] vector support as requested")
    println()
    println("Files generated with ARGB support:")
    println("  - rainbow_palette_matrix.txt (sample [255][4] matrix)")
    println("  - Various fractal renders with ARGB data")
    println("  - Vector matrices showing [height][width][4] format")
    println("  - Transparency maps for alpha channel visualization")
    println("  - All palette matrices in [255][4] format")
}