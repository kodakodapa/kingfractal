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

data class BuddhabrotParams(
    val zoom: Float = 1.0f,
    val centerX: Float = -0.5f,
    val centerY: Float = 0.0f,
    val maxIterations: Int = 1000,
    val sampleCount: Int = 1000000
) : FractalParams()

// IFS (Iterated Function System) parameters
sealed class IFSParams : FractalParams()

data class SierpinskiTriangleParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val iterations: Int = 100000,
    val pointSize: Int = 1
) : IFSParams()
