package com.github.kodakodapa.kingfractal.gui

import com.github.kodakodapa.kingfractal.utils.MandelbrotParams
import com.github.kodakodapa.kingfractal.utils.JuliaParams
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Rectangle

class MouseSelectionTest {

    @Test
    fun `should update MandelbrotParams with new coordinates`() {
        val originalParams = MandelbrotParams(
            zoom = 1.0f,
            centerX = -0.5f,
            centerY = 0.0f,
            maxIterations = 100
        )
        
        val updatedParams = originalParams.withNewParams(2.0f, 0.0f, 0.5f)
        
        assertEquals(2.0f, updatedParams.zoom)
        assertEquals(0.0f, updatedParams.centerX)
        assertEquals(0.5f, updatedParams.centerY)
        assertEquals(100, updatedParams.maxIterations)
    }

    @Test
    fun `should update JuliaParams with new coordinates`() {
        val originalParams = JuliaParams(
            zoom = 1.0f,
            centerX = 0.0f,
            centerY = 0.0f,
            juliaReal = -0.7f,
            juliaImag = 0.27015f,
            maxIterations = 100
        )
        
        val updatedParams = originalParams.withNewParams(3.0f, 1.0f, -1.0f)
        
        assertEquals(3.0f, updatedParams.zoom)
        assertEquals(1.0f, updatedParams.centerX)
        assertEquals(-1.0f, updatedParams.centerY)
        assertEquals(-0.7f, (updatedParams as JuliaParams).juliaReal)
        assertEquals(0.27015f, updatedParams.juliaImag)
        assertEquals(100, updatedParams.maxIterations)
    }

    @Test
    fun `should preserve Julia-specific parameters when updating coordinates`() {
        val originalParams = JuliaParams(
            zoom = 1.0f,
            centerX = 0.0f,
            centerY = 0.0f,
            juliaReal = -0.8f,
            juliaImag = 0.156f,
            maxIterations = 150
        )
        
        val updatedParams = originalParams.withNewParams(2.0f, 0.5f, 0.5f) as JuliaParams
        
        // Julia-specific parameters should be preserved
        assertEquals(-0.8f, updatedParams.juliaReal)
        assertEquals(0.156f, updatedParams.juliaImag)
        assertEquals(150, updatedParams.maxIterations)
        
        // Updated parameters should be changed
        assertEquals(2.0f, updatedParams.zoom)
        assertEquals(0.5f, updatedParams.centerX)
        assertEquals(0.5f, updatedParams.centerY)
    }
}