package com.github.kodakodapa.kingfractal.threedimensional

data class Ray(val origin: Vector3, val direction: Vector3) {

    fun pointAt(t: Double): Vector3 = origin + direction * t
}