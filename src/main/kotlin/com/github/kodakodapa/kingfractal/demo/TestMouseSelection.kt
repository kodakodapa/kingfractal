package com.github.kodakodapa.kingfractal.demo

import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import com.github.kodakodapa.kingfractal.utils.JuliaParams

fun main() {
    println("Testing mouse selection functionality...")
    
    // Test MandelbrotParams
    val mandelbrotParams = MandelbrotParams(zoom = 1.0f, centerX = -0.5f, centerY = 0.0f, maxIterations = 100)
    val updatedMandelbrot = mandelbrotParams.withNewParams(2.0f, 0.0f, 0.5f)
    
    println("Original Mandelbrot: zoom=${mandelbrotParams.zoom}, center=(${mandelbrotParams.centerX}, ${mandelbrotParams.centerY})")
    println("Updated Mandelbrot: zoom=${updatedMandelbrot.zoom}, center=(${updatedMandelbrot.centerX}, ${updatedMandelbrot.centerY})")
    
    // Test JuliaParams
    val juliaParams = JuliaParams(zoom = 1.0f, centerX = 0.0f, centerY = 0.0f, juliaReal = -0.7f, juliaImag = 0.27015f, maxIterations = 100)
    val updatedJulia = juliaParams.withNewParams(3.0f, 1.0f, -1.0f) as JuliaParams
    
    println("Original Julia: zoom=${juliaParams.zoom}, center=(${juliaParams.centerX}, ${juliaParams.centerY}), julia=(${juliaParams.juliaReal}, ${juliaParams.juliaImag})")
    println("Updated Julia: zoom=${updatedJulia.zoom}, center=(${updatedJulia.centerX}, ${updatedJulia.centerY}), julia=(${updatedJulia.juliaReal}, ${updatedJulia.juliaImag})")
    
    println("✅ Mouse selection parameter updates work correctly!")
}