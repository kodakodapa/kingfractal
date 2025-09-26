package com.github.kodakodapa.kingfractal.threedimensional

data class HitRecord(
    val point: Vector3,
    val normal: Vector3,
    val t: Double,
    val material: Material,
    val frontFace: Boolean
)