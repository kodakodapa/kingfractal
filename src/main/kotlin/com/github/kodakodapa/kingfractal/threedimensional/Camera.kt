package com.github.kodakodapa.kingfractal.threedimensional

import kotlin.math.*

class Camera(
    val position: Vector3,
    val target: Vector3,
    val up: Vector3,
    val fov: Double,
    val aspectRatio: Double
) {
    private val w = (position - target).normalize()
    private val u = up.cross(w).normalize()
    private val v = w.cross(u)
    private val halfHeight = tan(fov * PI / 180.0 / 2.0)
    private val halfWidth = aspectRatio * halfHeight
    private val lowerLeftCorner = position - u * halfWidth - v * halfHeight - w

    fun getRay(s: Double, t: Double): Ray {
        return Ray(position, lowerLeftCorner + u * (s * halfWidth * 2) + v * (t * halfHeight * 2) - position)
    }
}