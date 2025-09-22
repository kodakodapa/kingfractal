package com.github.kodakodapa.kingfractal.ifs

import com.github.kodakodapa.kingfractal.outputs.ImageData
import com.github.kodakodapa.kingfractal.outputs.ARGB_CHANNELS
import com.github.kodakodapa.kingfractal.utils.IFSParams
import com.github.kodakodapa.kingfractal.utils.SierpinskiTriangleParams
import kotlin.random.Random

/**
 * CPU-based renderer for IFS (Iterated Function System) fractals
 */
class IFSRenderer {

    fun renderIFS(width: Int, height: Int, params: IFSParams): ImageData {
        return when (params) {
            is SierpinskiTriangleParams -> renderSierpinskiTriangle(width, height, params)
        }
    }

    private fun renderSierpinskiTriangle(width: Int, height: Int, params: SierpinskiTriangleParams): ImageData {
        // Create histogram to count point visits
        val histogram = IntArray(width * height) { 0 }

        // Define the three vertices of the Sierpinski triangle
        val vertices = arrayOf(
            Pair(0.0, 0.0),        // Bottom left
            Pair(1.0, 0.0),        // Bottom right
            Pair(0.5, 0.866)       // Top (equilateral triangle height)
        )

        // Start at a random point inside the triangle
        var x = 0.5
        var y = 0.3

        val random = Random.Default

        // Iterate the chaos game
        for (i in 0 until params.iterations) {
            // Choose a random vertex
            val vertex = vertices[random.nextInt(3)]

            // Move halfway toward the chosen vertex
            x = (x + vertex.first) / 2.0
            y = (y + vertex.second) / 2.0

            // Transform to screen coordinates with zoom and centering
            val screenX = ((x - 0.5) * params.zoom + params.centerX + 0.5) * width
            val screenY = ((0.866 - y) * params.zoom + params.centerY + 0.5) * height

            // Check bounds and plot point
            if (screenX >= 0 && screenX < width && screenY >= 0 && screenY < height) {
                val pixelIndex = screenY.toInt() * width + screenX.toInt()
                if (pixelIndex in histogram.indices) {
                    histogram[pixelIndex]++
                }
            }
        }

        return convertHistogramToImageData(histogram, width, height)
    }

    private fun convertHistogramToImageData(histogram: IntArray, width: Int, height: Int): ImageData {
        // Find max hits for normalization
        val maxHits = histogram.maxOrNull() ?: 1

        // Create ARGB byte array
        val pixels = ByteArray(width * height * ARGB_CHANNELS)

        for (i in histogram.indices) {
            val hits = histogram[i]

            // Use logarithmic scaling for better visualization
            val value = if (hits > 0) {
                val logHits = kotlin.math.ln(1.0 + hits.toDouble())
                val logMax = kotlin.math.ln(1.0 + maxHits.toDouble())
                ((logHits / logMax) * 255.0).toInt().coerceIn(1, 255)
            } else {
                0
            }

            val pixelIndex = i * ARGB_CHANNELS
            pixels[pixelIndex] = value.toByte()     // A
            pixels[pixelIndex + 1] = value.toByte() // R
            pixels[pixelIndex + 2] = value.toByte() // G
            pixels[pixelIndex + 3] = value.toByte() // B
        }

        return ImageData.fromByteArray(width, height, pixels)
    }
}