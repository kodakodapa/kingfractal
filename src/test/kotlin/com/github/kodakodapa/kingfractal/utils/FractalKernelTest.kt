package com.github.kodakodapa.kingfractal.utils

import com.github.kodakodapa.kingfractal.utils.FractalKernels
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FractalKernelsTest {

    @Test
    fun `mandelbrot kernel should contain required functions`() {
        val kernel = FractalKernels.mandelbrotKernel

        assertTrue(kernel.contains("__kernel void mandelbrot"))
        assertTrue(kernel.contains("get_global_id"))
        assertTrue(kernel.contains("output"))
        assertTrue(kernel.contains("width"))
        assertTrue(kernel.contains("height"))
        assertTrue(kernel.contains("zoom"))
        assertTrue(kernel.contains("maxIterations"))
    }

    @Test
    fun `julia kernel should contain required functions`() {
        val kernel = FractalKernels.juliaKernel

        assertTrue(kernel.contains("__kernel void julia"))
        assertTrue(kernel.contains("get_global_id"))
        assertTrue(kernel.contains("output"))
        assertTrue(kernel.contains("width"))
        assertTrue(kernel.contains("height"))
        assertTrue(kernel.contains("juliaReal"))
        assertTrue(kernel.contains("juliaImag"))
        assertTrue(kernel.contains("maxIterations"))
    }

    @Test
    fun `kernels should have different implementations`() {
        val mandelbrotKernel = FractalKernels.mandelbrotKernel
        val juliaKernel = FractalKernels.juliaKernel

        assertNotEquals(mandelbrotKernel, juliaKernel)
        assertTrue(mandelbrotKernel.isNotEmpty())
        assertTrue(juliaKernel.isNotEmpty())
    }
}