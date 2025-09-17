package com.github.kodakodapa.kingfractal.fractal

import com.github.kodakodapa.kingfractal.color.ARGBColor
import com.github.kodakodapa.kingfractal.color.ARGBColorMatrix
import com.github.kodakodapa.kingfractal.palette.ARGBPalette

/**
 * Parameters for fractal rendering with ARGB support
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
 * Result class that holds both ARGB colors and raw vectors for [255][4] support
 */
class ARGBFractalResult(
    val width: Int,
    val height: Int
) {
    private val colors = Array(height) { Array(width) { ARGBColor.BLACK } }
    private val vectors = Array(height) { Array(width) { intArrayOf(255, 0, 0, 0) } }
    
    /**
     * Set pixel color and automatically update vector representation
     */
    fun setPixel(x: Int, y: Int, color: ARGBColor) {
        colors[y][x] = color
        vectors[y][x] = color.toVector()
    }
    
    /**
     * Get pixel color
     */
    fun getPixel(x: Int, y: Int): ARGBColor = colors[y][x]
    
    /**
     * Get pixel as [A, R, G, B] vector
     */
    fun getPixelVector(x: Int, y: Int): IntArray = vectors[y][x].copyOf()
    
    /**
     * Get entire row of colors
     */
    fun getRow(y: Int): Array<ARGBColor> = colors[y].copyOf()
    
    /**
     * Get entire row as vectors [255][4]
     */
    fun getRowVectors(y: Int): Array<IntArray> = vectors[y].map { it.copyOf() }.toTypedArray()
    
    /**
     * Get the entire result as a matrix of colors
     */
    fun getColorMatrix(): Array<Array<ARGBColor>> = colors.map { it.copyOf() }.toTypedArray()
    
    /**
     * Get the entire result as [height][width][4] vector array
     * This provides the requested [255][4] size vectors format for the entire image
     */
    fun getVectorMatrix(): Array<Array<IntArray>> = 
        vectors.map { row -> row.map { it.copyOf() }.toTypedArray() }.toTypedArray()
    
    /**
     * Convert to packed ARGB integer array for compatibility
     */
    fun toPackedARGBMatrix(): Array<IntArray> = 
        colors.map { row -> row.map { it.toPackedARGB() }.toIntArray() }.toTypedArray()
}

/**
 * CPU-based Mandelbrot renderer with full ARGB support
 */
class ARGBMandelbrotRenderer {
    
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
     * Render a Mandelbrot fractal using the specified ARGB palette
     */
    fun render(params: FractalParams, palette: ARGBPalette): ARGBFractalResult {
        val result = ARGBFractalResult(params.width, params.height)
        
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
                val color = palette.getColor(iterations, params.maxIterations)
                result.setPixel(x, y, color)
            }
        }
        
        return result
    }
    
    /**
     * Render and return as [255][4] color matrix for the requested functionality
     */
    fun renderAsColorMatrix(params: FractalParams, palette: ARGBPalette): ARGBColorMatrix {
        val paletteMatrix = palette.generateColorMatrix(params.maxIterations)
        return paletteMatrix
    }
}

/**
 * CPU-based Julia set renderer with full ARGB support
 */
class ARGBJuliaRenderer {
    
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
     * Render a Julia fractal using the specified ARGB palette
     */
    fun render(
        params: FractalParams, 
        palette: ARGBPalette, 
        juliaC: Complex = Complex(-0.7, 0.27015)
    ): ARGBFractalResult {
        val result = ARGBFractalResult(params.width, params.height)
        
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
                val color = palette.getColor(iterations, params.maxIterations)
                result.setPixel(x, y, color)
            }
        }
        
        return result
    }
}

/**
 * Multi-threaded fractal renderer for better performance
 */
class ARGBParallelRenderer {
    
    /**
     * Render Mandelbrot fractal using multiple threads
     */
    fun renderMandelbrotParallel(
        params: FractalParams, 
        palette: ARGBPalette,
        numThreads: Int = Runtime.getRuntime().availableProcessors()
    ): ARGBFractalResult {
        val result = ARGBFractalResult(params.width, params.height)
        val rowsPerThread = params.height / numThreads
        val threads = mutableListOf<Thread>()
        
        val scale = 4.0 / params.zoom
        val xMin = params.centerX - scale / 2
        val yMin = params.centerY - scale / 2
        val xStep = scale / params.width
        val yStep = scale / params.height
        
        for (threadId in 0 until numThreads) {
            val startRow = threadId * rowsPerThread
            val endRow = if (threadId == numThreads - 1) params.height else (threadId + 1) * rowsPerThread
            
            val thread = Thread {
                for (y in startRow until endRow) {
                    for (x in 0 until params.width) {
                        val real = xMin + x * xStep
                        val imag = yMin + y * yStep
                        val c = Complex(real, imag)
                        
                        val iterations = mandelbrotIterations(c, params.maxIterations)
                        val color = palette.getColor(iterations, params.maxIterations)
                        
                        synchronized(result) {
                            result.setPixel(x, y, color)
                        }
                    }
                }
            }
            
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        return result
    }
    
    private fun mandelbrotIterations(c: Complex, maxIterations: Int): Int {
        var z = Complex(0.0, 0.0)
        var iterations = 0
        
        while (z.magnitudeSquared() <= 4.0 && iterations < maxIterations) {
            z = z * z + c
            iterations++
        }
        
        return iterations
    }
}