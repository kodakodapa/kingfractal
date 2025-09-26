package com.github.kodakodapa.kingfractal.threedimensional

interface Hittable {
    fun hit(ray: Ray, tMin: Double, tMax: Double): HitRecord?
}