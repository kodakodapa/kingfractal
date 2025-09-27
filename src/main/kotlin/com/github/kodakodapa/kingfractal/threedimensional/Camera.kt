package com.github.kodakodapa.kingfractal.threedimensional

import kotlin.math.*

class Camera(
    var position: Vector3,
    var target: Vector3,
    val up: Vector3,
    val fov: Double,
    val aspectRatio: Double,
    var moveSpeed: Double = 0.1,
    var turnSpeed: Double = 0.05
) {
    private var w = (position - target).normalize()
    private var u = up.cross(w).normalize()
    private var v = w.cross(u)
    private val halfHeight = tan(fov * PI / 180.0 / 2.0)
    private val halfWidth = aspectRatio * halfHeight
    private var lowerLeftCorner = position - u * halfWidth - v * halfHeight - w

    private fun updateVectors() {
        w = (position - target).normalize()
        u = up.cross(w).normalize()
        v = w.cross(u)
        lowerLeftCorner = position - u * halfWidth - v * halfHeight - w
    }

    fun moveForward() {
        val direction = (target - position).normalize()
        position = position + direction * moveSpeed
        target = target + direction * moveSpeed
        updateVectors()
    }

    fun moveBackward() {
        val direction = (target - position).normalize()
        position = position - direction * moveSpeed
        target = target - direction * moveSpeed
        updateVectors()
    }

    fun turnLeft() {
        val direction = (target - position)
        val cosAngle = cos(turnSpeed)
        val sinAngle = sin(turnSpeed)
        val newDirection = Vector3(
            direction.x * cosAngle + direction.z * sinAngle,
            direction.y,
            -direction.x * sinAngle + direction.z * cosAngle
        )
        target = position + newDirection
        updateVectors()
    }

    fun turnRight() {
        val direction = (target - position)
        val cosAngle = cos(-turnSpeed)
        val sinAngle = sin(-turnSpeed)
        val newDirection = Vector3(
            direction.x * cosAngle + direction.z * sinAngle,
            direction.y,
            -direction.x * sinAngle + direction.z * cosAngle
        )
        target = position + newDirection
        updateVectors()
    }

    fun handleKeyPress(key: Char) {
        when (key.lowercaseChar()) {
            'w' -> moveForward()
            's' -> moveBackward()
            'a' -> turnLeft()
            'd' -> turnRight()
        }
    }

    fun getRay(s: Double, t: Double): Ray {
        return Ray(position, lowerLeftCorner + u * (s * halfWidth * 2) + v * (t * halfHeight * 2) - position)
    }
}