package org.example.com.github.kodakodapa.kingfractal
import org.example.com.github.kodakodapa.kingfractal.utils.FractalKernels
import org.example.com.github.kodakodapa.kingfractal.utils.ImageData
import org.example.com.github.kodakodapa.kingfractal.utils.JuliaParams
import org.example.com.github.kodakodapa.kingfractal.utils.MandelbrotParams

fun main() {
    val width = 1920
    val height = 1080

    // Create Mandelbrot renderer
    val mandelbrotRenderer = OpenCLRenderer<ImageData>(
        kernelSource = FractalKernels.mandelbrotKernel,
        dataFactory = { bytes -> ImageData.fromByteArray(width, height, bytes) }
    )

    // Create Julia renderer
    val juliaRenderer = OpenCLRenderer<ImageData>(
        kernelSource = FractalKernels.juliaKernel,
        dataFactory = { bytes -> ImageData.fromByteArray(width, height, bytes) }
    )

    try {
        // Initialize both renderers
        println("=== Initializing Mandelbrot Renderer ===")
        mandelbrotRenderer.initialize()

        println("\n=== Initializing Julia Renderer ===")
        juliaRenderer.initialize()

        // Render Mandelbrot set
        println("\n=== Rendering Mandelbrot Set ===")
        val mandelbrotParams = MandelbrotParams(
            zoom = 1.0f,
            centerX = -0.5f,
            centerY = 0.0f,
            maxIterations = 100
        )

        val mandelBrotImage = ImageData.fromDimensions(width, height)
        val mandelbrotResult = mandelbrotRenderer.execute(
            mandelBrotImage,
            width, height,
            mandelbrotParams.zoom,
            mandelbrotParams.centerX,
            mandelbrotParams.centerY,
            mandelbrotParams.maxIterations
        )

        // Render Julia set
        println("\n=== Rendering Julia Set ===")
        val juliaParams = JuliaParams(
            zoom = 1.0f,
            centerX = 0.0f,
            centerY = 0.0f,
            juliaReal = -0.7f,
            juliaImag = 0.27015f,
            maxIterations = 100
        )

        val juliaImage = ImageData.fromDimensions(width, height)
        val juliaResult = juliaRenderer.execute(
            juliaImage,
            width, height,
            juliaParams.zoom,
            juliaParams.centerX,
            juliaParams.centerY,
            juliaParams.juliaReal,
            juliaParams.juliaImag,
            juliaParams.maxIterations
        )

        mandelbrotResult.saveAsPng("mandelbrot_${System.currentTimeMillis()}")
        juliaResult.saveAsPng("julia_${System.currentTimeMillis()}")

    } finally {
        mandelbrotRenderer.cleanup()
        juliaRenderer.cleanup()
    }
}