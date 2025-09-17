package com.github.kodakodapa.kingfractal.fractal

import com.github.kodakodapa.kingfractal.palette.ColorPalette
import kotlin.math.sqrt

/**
 * Parameters for fractal rendering
 */
data class FractalParams(
    val width: Int,
    val height: Int,
    val centerX: Double = -0.5,
    val centerY: Double = 0.0,
    val zoom: Double = 1.0,
    val maxIterations: Int = 100
)

/**
 * Represents a point in the complex plane
 */
data class Complex(val real: Double, val imag: Double) {
    operator fun plus(other: Complex) = Complex(real + other.real, imag + other.imag)
    operator fun times(other: Complex) = Complex(
        real * other.real - imag * other.imag,
        real * other.imag + imag * other.real
    )
    fun magnitudeSquared() = real * real + imag * imag
}

/**
 * Simple CPU-based fractal renderer (can be extended with OpenCL later)
 */
class MandelbrotRenderer {
    
    /**
     * Calculate the number of iterations for a point to escape the Mandelbrot set
     */
    private fun mandelbrotIterations(c: Complex, maxIterations: Int): Int {
        var z = Complex(0.0, 0.0)
        var iterations = 0
        
        while (z.magnitudeSquared() <= 4.0 && iterations < maxIterations) {
            z = z * z + c
            iterations++
        }
        
        return iterations
    }
    
    /**
     * Render a Mandelbrot fractal using the specified palette
     */
    fun render(params: FractalParams, palette: ColorPalette): Array<IntArray> {
        val result = Array(params.height) { IntArray(params.width) }
        
        val scale = 4.0 / params.zoom
        val xMin = params.centerX - scale / 2
        val yMin = params.centerY - scale / 2
        val xStep = scale / params.width
        val yStep = scale / params.height
        
        for (y in 0 until params.height) {
            for (x in 0 until params.width) {
                val real = xMin + x * xStep
                val imag = yMin + y * yStep
                val c = Complex(real, imag)
                
                val iterations = mandelbrotIterations(c, params.maxIterations)
                result[y][x] = palette.getColor(iterations, params.maxIterations)
            }
        }
        
        return result
    }
}

/**
 * Julia set renderer
 */
class JuliaRenderer {
    
    /**
     * Calculate the number of iterations for a point to escape the Julia set
     */
    private fun juliaIterations(z: Complex, c: Complex, maxIterations: Int): Int {
        var currentZ = z
        var iterations = 0
        
        while (currentZ.magnitudeSquared() <= 4.0 && iterations < maxIterations) {
            currentZ = currentZ * currentZ + c
            iterations++
        }
        
        return iterations
    }
    
    /**
     * Render a Julia fractal using the specified palette
     */
    fun render(params: FractalParams, palette: ColorPalette, juliaC: Complex = Complex(-0.7, 0.27015)): Array<IntArray> {
        val result = Array(params.height) { IntArray(params.width) }
        
        val scale = 4.0 / params.zoom
        val xMin = params.centerX - scale / 2
        val yMin = params.centerY - scale / 2
        val xStep = scale / params.width
        val yStep = scale / params.height
        
        for (y in 0 until params.height) {
            for (x in 0 until params.width) {
                val real = xMin + x * xStep
                val imag = yMin + y * yStep
                val z = Complex(real, imag)
                
                val iterations = juliaIterations(z, juliaC, params.maxIterations)
                result[y][x] = palette.getColor(iterations, params.maxIterations)
            }
        }
        
        return result
    }
}