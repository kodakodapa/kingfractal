package com.github.kodakodapa.kingfractal.utils

import com.github.kodakodapa.kingfractal.utils.JuliaParams
import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test



class FractalParamsTest {

    @Test
    fun `should create MandelbrotParams with custom values`() {
        val params = MandelbrotParams(
            zoom = 2.0f,
            centerX = 1.0f,
            centerY = -1.0f,
            maxIterations = 200
        )

        assertEquals(2.0f, params.zoom)
        assertEquals(1.0f, params.centerX)
        assertEquals(-1.0f, params.centerY)
        assertEquals(200, params.maxIterations)
    }

    @Test
    fun `should create JuliaParams with custom values`() {
        val params = JuliaParams(
            zoom = 1.5f,
            centerX = 0.5f,
            centerY = 0.5f,
            juliaReal = -0.8f,
            juliaImag = 0.156f,
            maxIterations = 150
        )

        assertEquals(1.5f, params.zoom)
        assertEquals(0.5f, params.centerX)
        assertEquals(0.5f, params.centerY)
        assertEquals(-0.8f, params.juliaReal)
        assertEquals(0.156f, params.juliaImag)
        assertEquals(150, params.maxIterations)
    }
}