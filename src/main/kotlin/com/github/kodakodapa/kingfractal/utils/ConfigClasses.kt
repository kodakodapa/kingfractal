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
    val weight2: Float = 0.5f,
    // Variation types (0=linear, 1=sinusoidal, 2=spherical, 3=swirl, 4=horseshoe, 5=polar, 6=handkerchief, 7=heart, 8=disc, 9=spiral, 10=hyperbolic, 11=diamond)
    val variation1: Int = 0,
    val variation2: Int = 1,
    // Variation weights
    val varWeight1: Float = 1.0f,
    val varWeight2: Float = 1.0f
) : FractalParams()

// Variation types enum for reference
enum class FlameVariationType(val id: Int, val displayName: String) {
    LINEAR(0, "Linear"),
    SINUSOIDAL(1, "Sinusoidal"),
    SPHERICAL(2, "Spherical"),
    SWIRL(3, "Swirl"),
    HORSESHOE(4, "Horseshoe"),
    POLAR(5, "Polar"),
    HANDKERCHIEF(6, "Handkerchief"),
    HEART(7, "Heart"),
    DISC(8, "Disc"),
    SPIRAL(9, "Spiral"),
    HYPERBOLIC(10, "Hyperbolic"),
    DIAMOND(11, "Diamond")
}
