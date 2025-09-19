package com.github.kodakodapa.kingfractal.outputs

import com.github.kodakodapa.kingfractal.colors.ARGBPaletteRegistry

fun main() {
    val renderer = PaletteRender()

    // Save all palettes as a single comprehensive image
    println("Rendering all palettes from registry...")
    renderer.saveAllPalettesAsPng("all_palettes_visualization.png")

    // Save individual palette visualizations
    val palettes = ARGBPaletteRegistry.getAllPalettes()
    println("\nRendering individual palettes:")

    palettes.forEach { palette ->
        val filename = "palette_${palette.name.replace(" ", "_").replace("(", "").replace(")", "")}.png"
        println("  - ${palette.name} -> $filename")
        renderer.saveAsPng(filename, palette)
    }

    println("\nVisualization complete!")
    println("The visualizations show:")
    println("  - Top row: RGB colors only (alpha ignored)")
    println("  - Middle row: Alpha channel as grayscale")
    println("  - Bottom row: Full ARGB with checkerboard background")
    println("  - Grid: Color swatches (left=RGB, right=ARGB with transparency)")
}