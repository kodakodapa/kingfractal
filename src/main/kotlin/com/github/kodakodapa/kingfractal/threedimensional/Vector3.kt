package com.github.kodakodapa.kingfractal.threedimensional

import kotlin.math.*

data class Vector3(val x: Double, val y: Double, val z: Double) {

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Double) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Double) = Vector3(x / scalar, y / scalar, z / scalar)

    fun dot(other: Vector3) = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3) = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3 {
        val len = length()
        if (len == 0.0) {
            throw IllegalStateException("Cannot normalize zero-length vector: division by zero would occur.")
        }
        return this / len
    }

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
        val UNIT_X = Vector3(1.0, 0.0, 0.0)
        val UNIT_Y = Vector3(0.0, 1.0, 0.0)
        val UNIT_Z = Vector3(0.0, 0.0, 1.0)
    }
}