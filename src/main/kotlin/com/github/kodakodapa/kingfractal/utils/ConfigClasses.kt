package com.github.kodakodapa.kingfractal.utils

sealed class FractalParams

// Fractal-specific configuration classes
data class MandelbrotParams(
    val zoom: Float = 1.0f,
    val centerX: Float = -0.5f,
    val centerY: Float = 0.0f,
    val maxIterations: Int = 100
) : FractalParams()

data class JuliaParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val juliaReal: Float = -0.7f,
    val juliaImag: Float = 0.27015f,
    val maxIterations: Int = 100
) : FractalParams()
