package com.github.kodakodapa.kingfractal.utils

sealed class FractalParams {
    abstract val zoom: Float
    abstract val centerX: Float
    abstract val centerY: Float
    abstract val maxIterations: Int
    
    abstract fun withNewParams(zoom: Float, centerX: Float, centerY: Float): FractalParams
}

// Fractal-specific configuration classes
data class MandelbrotParams(
    override val zoom: Float = 1.0f,
    override val centerX: Float = -0.5f,
    override val centerY: Float = 0.0f,
    override val maxIterations: Int = 100
) : FractalParams() {
    override fun withNewParams(zoom: Float, centerX: Float, centerY: Float): FractalParams {
        return copy(zoom = zoom, centerX = centerX, centerY = centerY)
    }
}

data class JuliaParams(
    override val zoom: Float = 1.0f,
    override val centerX: Float = 0.0f,
    override val centerY: Float = 0.0f,
    val juliaReal: Float = -0.7f,
    val juliaImag: Float = 0.27015f,
    override val maxIterations: Int = 100
) : FractalParams() {
    override fun withNewParams(zoom: Float, centerX: Float, centerY: Float): FractalParams {
        return copy(zoom = zoom, centerX = centerX, centerY = centerY)
    }
}
