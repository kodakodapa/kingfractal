package com.github.kodakodapa.kingfractal.threedimensional

import com.github.kodakodapa.kingfractal.colors.ARGBColor
import kotlin.math.*
import kotlin.random.Random

// Data class for floating-point color accumulation
data class ColorAccumulator(
    var r: Double = 0.0,
    var g: Double = 0.0,
    var b: Double = 0.0
) {
    fun add(color: ARGBColor) {
        r += color.red.toDouble()
        g += color.green.toDouble()
        b += color.blue.toDouble()
    }

    fun toARGBColor(samples: Int): ARGBColor {
        // Average the samples first
        val avgR = r / samples
        val avgG = g / samples
        val avgB = b / samples

        // Apply gamma correction (gamma = 2.0, so sqrt)
        val correctedR = sqrt(avgR / 255.0)
        val correctedG = sqrt(avgG / 255.0)
        val correctedB = sqrt(avgB / 255.0)

        return ARGBColor(
            255,
            (correctedR * 255).toInt().coerceIn(0, 255),
            (correctedG * 255).toInt().coerceIn(0, 255),
            (correctedB * 255).toInt().coerceIn(0, 255)
        )
    }
}

class RayTracer {

    fun render(
        camera: Camera,
        world: HittableList,
        imageWidth: Int,
        imageHeight: Int,
        maxDepth: Int = 10,
        samplesPerPixel: Int = 10
    ): Array<Array<ARGBColor>> {
        val image = Array(imageHeight) { Array(imageWidth) { ARGBColor.BLACK } }

        for (j in 0 until imageHeight) {
            for (i in 0 until imageWidth) {
                val colorAccumulator = ColorAccumulator()

                for (s in 0 until samplesPerPixel) {
                    val u = (i + Random.nextDouble()) / (imageWidth - 1)
                    val v = (imageHeight - 1 - j + Random.nextDouble()) / (imageHeight - 1)
                    val ray = camera.getRay(u, v)
                    val color = rayColor(ray, world, maxDepth)
                    colorAccumulator.add(color)
                }

                // Convert accumulated color to final pixel color with proper averaging and gamma correction
                image[j][i] = colorAccumulator.toARGBColor(samplesPerPixel)
            }
        }

        return image
    }

    private fun rayColor(ray: Ray, world: Hittable, depth: Int): ARGBColor {
        if (depth <= 0) return ARGBColor.BLACK

        val hitRecord = world.hit(ray, 0.001, Double.POSITIVE_INFINITY)
        if (hitRecord != null) {
            // Simple diffuse shading
            val target = hitRecord.point + hitRecord.normal + randomUnitVector()
            val scattered = Ray(hitRecord.point, target - hitRecord.point)
            val attenuation = hitRecord.material.color
            val scatteredColor = rayColor(scattered, world, depth - 1)

            return ARGBColor(
                255,
                (attenuation.red * scatteredColor.red / 255.0).toInt().coerceIn(0, 255),
                (attenuation.green * scatteredColor.green / 255.0).toInt().coerceIn(0, 255),
                (attenuation.blue * scatteredColor.blue / 255.0).toInt().coerceIn(0, 255)
            )
        }

        // Sky gradient
        val unitDirection = ray.direction.normalize()
        val t = 0.5 * (unitDirection.y + 1.0)
        val skyColor1 = ARGBColor(255, 255, 255, 255) // white
        val skyColor2 = ARGBColor(255, 128, 178, 255) // light blue

        return lerp(skyColor1, skyColor2, t)
    }

    private fun randomUnitVector(): Vector3 {
        while (true) {
            val p = Vector3(
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0),
                Random.nextDouble(-1.0, 1.0)
            )
            if (p.length() < 1.0) return p.normalize()
        }
    }

    private fun lerp(a: ARGBColor, b: ARGBColor, t: Double): ARGBColor {
        return ARGBColor(
            255,
            ((1.0 - t) * a.red + t * b.red).toInt().coerceIn(0, 255),
            ((1.0 - t) * a.green + t * b.green).toInt().coerceIn(0, 255),
            ((1.0 - t) * a.blue + t * b.blue).toInt().coerceIn(0, 255)
        )
    }
}