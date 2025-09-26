package com.github.kodakodapa.kingfractal.threedimensional

import com.github.kodakodapa.kingfractal.colors.ARGBColor

data class Material(
    val color: ARGBColor,
    val reflectivity: Double = 0.0,
    val transparency: Double = 0.0,
    val refractiveIndex: Double = 1.0,
    val roughness: Double = 0.0
)