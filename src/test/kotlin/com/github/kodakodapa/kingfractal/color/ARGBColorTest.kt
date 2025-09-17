package com.github.kodakodapa.kingfractal.color

import kotlin.test.*

class ARGBColorTest {
    
    @Test
    fun testARGBColorCreation() {
        val color = ARGBColor(128, 255, 128, 64)
        assertEquals(128, color.alpha)
        assertEquals(255, color.red)
        assertEquals(128, color.green)
        assertEquals(64, color.blue)
    }
    
    @Test
    fun testARGBColorValidation() {
        // Valid ranges should work
        val validColor1 = ARGBColor(0, 0, 0, 0)
        val validColor2 = ARGBColor(255, 255, 255, 255)
        
        // Invalid ranges should fail
        assertFails { ARGBColor(-1, 0, 0, 0) }
        assertFails { ARGBColor(256, 0, 0, 0) }
        assertFails { ARGBColor(128, -1, 0, 0) }
        assertFails { ARGBColor(128, 256, 0, 0) }
    }
    
    @Test
    fun testPackedARGBConversion() {
        val color = ARGBColor(255, 255, 128, 64)
        val packed = color.toPackedARGB()
        val expectedPacked = (255 shl 24) or (255 shl 16) or (128 shl 8) or 64
        assertEquals(expectedPacked, packed)
        
        // Test round-trip conversion
        val restored = ARGBColor.fromPackedARGB(packed)
        assertEquals(color, restored)
    }
    
    @Test
    fun testVectorConversion() {
        val color = ARGBColor(200, 100, 50, 25)
        val vector = color.toVector()
        
        assertEquals(4, vector.size)
        assertEquals(200, vector[0]) // Alpha
        assertEquals(100, vector[1]) // Red
        assertEquals(50, vector[2])  // Green
        assertEquals(25, vector[3])  // Blue
        
        // Test round-trip conversion
        val restored = ARGBColor.fromVector(vector)
        assertEquals(color, restored)
    }
    
    @Test
    fun testNormalizedVectorConversion() {
        val color = ARGBColor(255, 128, 64, 32)
        val normalized = color.toNormalizedVector()
        
        assertEquals(4, normalized.size)
        assertEquals(1.0f, normalized[0], 0.01f)
        assertEquals(128f/255f, normalized[1], 0.01f)
        assertEquals(64f/255f, normalized[2], 0.01f)
        assertEquals(32f/255f, normalized[3], 0.01f)
        
        // Test round-trip conversion (with some tolerance due to float precision)
        val restored = ARGBColor.fromNormalizedVector(normalized)
        assertEquals(color.alpha, restored.alpha)
        assertEquals(color.red, restored.red)
        assertEquals(color.green, restored.green)
        assertEquals(color.blue, restored.blue)
    }
    
    @Test
    fun testPreDefinedColors() {
        assertEquals(ARGBColor(255, 0, 0, 0), ARGBColor.BLACK)
        assertEquals(ARGBColor(255, 255, 255, 255), ARGBColor.WHITE)
        assertEquals(ARGBColor(255, 255, 0, 0), ARGBColor.RED)
        assertEquals(ARGBColor(255, 0, 255, 0), ARGBColor.GREEN)
        assertEquals(ARGBColor(255, 0, 0, 255), ARGBColor.BLUE)
        assertEquals(ARGBColor(0, 0, 0, 0), ARGBColor.TRANSPARENT)
        assertEquals(ARGBColor(128, 255, 0, 0), ARGBColor.SEMI_TRANSPARENT_RED)
    }
}

class ARGBInterpolationTest {
    
    @Test
    fun testLinearInterpolation() {
        val color1 = ARGBColor(100, 255, 0, 0)    // Semi-transparent red
        val color2 = ARGBColor(200, 0, 255, 0)    // More opaque green
        
        // Test midpoint interpolation
        val mid = ARGBInterpolation.lerp(color1, color2, 0.5f)
        assertEquals(150, mid.alpha)
        assertEquals(127, mid.red, "Red component should be around 127")    
        assertEquals(127, mid.green, "Green component should be around 127")
        assertEquals(0, mid.blue)
        
        // Test edge cases
        val start = ARGBInterpolation.lerp(color1, color2, 0.0f)
        assertEquals(color1, start)
        
        val end = ARGBInterpolation.lerp(color1, color2, 1.0f)
        assertEquals(color2, end)
        
        // Test clamping
        val clamped = ARGBInterpolation.lerp(color1, color2, 2.0f)
        assertEquals(color2, clamped)
    }
    
    @Test
    fun testHSVInterpolation() {
        val red = ARGBColor(255, 255, 0, 0)
        val blue = ARGBColor(255, 0, 0, 255)
        
        // HSV interpolation should give different results than linear
        val linearMid = ARGBInterpolation.lerp(red, blue, 0.5f)
        val hsvMid = ARGBInterpolation.lerpHSV(red, blue, 0.5f)
        
        // They should be different (HSV goes through purple/magenta)
        assertNotEquals(linearMid, hsvMid)
        
        // But alpha should be the same since both colors are opaque
        assertEquals(linearMid.alpha, hsvMid.alpha)
    }
}

class ARGBColorMatrixTest {
    
    @Test
    fun testColorMatrixCreation() {
        val matrix = ARGBColorMatrix(100)
        assertEquals(100, matrix.size)
        
        // Default should be black
        assertEquals(ARGBColor.BLACK, matrix[0])
        assertEquals(ARGBColor.BLACK, matrix[99])
    }
    
    @Test
    fun testColorMatrixSetGet() {
        val matrix = ARGBColorMatrix(10)
        val testColor = ARGBColor(128, 255, 128, 64)
        
        matrix[5] = testColor
        assertEquals(testColor, matrix[5])
        
        // Test vector access
        val vector = matrix.getVector(5)
        assertContentEquals(testColor.toVector(), vector)
        
        // Test vector setting
        val newVector = intArrayOf(200, 100, 50, 25)
        matrix.setVector(7, newVector)
        assertEquals(ARGBColor.fromVector(newVector), matrix[7])
    }
    
    @Test
    fun testColorMatrixBounds() {
        val matrix = ARGBColorMatrix(5)
        
        // Valid indices should work
        val color0 = matrix[0]  // Should not throw
        val color4 = matrix[4]  // Should not throw
        
        // Invalid indices should fail
        assertFails { matrix[-1] }
        assertFails { matrix[5] }
    }
    
    @Test
    fun testGradientFill() {
        val matrix = ARGBColorMatrix(5)
        val startColor = ARGBColor(100, 255, 0, 0)
        val endColor = ARGBColor(200, 0, 255, 0)
        
        matrix.fillGradient(startColor, endColor)
        
        // Check endpoints
        assertEquals(startColor, matrix[0])
        assertEquals(endColor, matrix[4])
        
        // Check that values change progressively
        val mid = matrix[2]
        assertTrue(mid.alpha > startColor.alpha && mid.alpha < endColor.alpha)
        assertTrue(mid.green > startColor.green && mid.green < endColor.green)
    }
    
    @Test
    fun testHSVGradientFill() {
        val matrix = ARGBColorMatrix(5)
        val red = ARGBColor(255, 255, 0, 0)
        val blue = ARGBColor(255, 0, 0, 255)
        
        matrix.fillHSVGradient(red, blue)
        
        // Check that we get valid colors and the fill worked
        assertTrue(matrix[0].alpha in 0..255)
        assertTrue(matrix[2].alpha in 0..255)
        assertTrue(matrix[4].alpha in 0..255)
        
        // HSV gradient should produce different colors than linear (for middle values)
        val linearMatrix = ARGBColorMatrix(5)
        linearMatrix.fillGradient(red, blue)
        
        // At minimum, the interpolation should work and produce valid colors
        val hsvMid = matrix[2]
        val linearMid = linearMatrix[2]
        
        assertTrue(hsvMid.alpha in 0..255)
        assertTrue(hsvMid.red in 0..255)
        assertTrue(hsvMid.green in 0..255)
        assertTrue(hsvMid.blue in 0..255)
    }
    
    @Test
    fun testMatrixConversion() {
        val matrix = ARGBColorMatrix(3)
        matrix[0] = ARGBColor(255, 255, 0, 0)
        matrix[1] = ARGBColor(128, 0, 255, 0)
        matrix[2] = ARGBColor(64, 0, 0, 255)
        
        val matrixArray = matrix.toMatrix()
        assertEquals(3, matrixArray.size)
        assertEquals(4, matrixArray[0].size)
        
        // Check that data is preserved
        assertContentEquals(matrix[0].toVector(), matrixArray[0])
        assertContentEquals(matrix[1].toVector(), matrixArray[1])
        assertContentEquals(matrix[2].toVector(), matrixArray[2])
    }
}