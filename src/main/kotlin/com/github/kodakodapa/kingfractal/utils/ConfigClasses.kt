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

// Fractal Flame parameters
data class FractalFlameParams(
    val zoom: Float = 1.0f,
    val centerX: Float = 0.0f,
    val centerY: Float = 0.0f,
    val iterations: Int = 1000000,
    val samples: Int = 50,
    val gamma: Float = 2.2f,
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f,
    // Transform coefficients for first affine transform
    val a1: Float = 0.5f,
    val b1: Float = 0.0f,
    val c1: Float = 0.0f,
    val d1: Float = 0.5f,
    val e1: Float = 0.0f,
    val f1: Float = 0.0f,
    // Transform coefficients for second affine transform
    val a2: Float = -0.5f,
    val b2: Float = 0.0f,
    val c2: Float = 0.5f,
    val d2: Float = 0.5f,
    val e2: Float = 0.5f,
    val f2: Float = 0.0f,
    // Transform weights
    val weight1: Float = 0.5f,
    val weight2: Float = 0.5f
) : FractalParams()
