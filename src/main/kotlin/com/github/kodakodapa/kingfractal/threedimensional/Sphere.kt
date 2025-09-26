package com.github.kodakodapa.kingfractal.threedimensional

import kotlin.math.*

class Sphere(
    private val center: Vector3,
    private val radius: Double,
    private val material: Material
) : Hittable {

    override fun hit(ray: Ray, tMin: Double, tMax: Double): HitRecord? {
        val oc = ray.origin - center
        val a = ray.direction.dot(ray.direction)
        val halfB = oc.dot(ray.direction)
        val c = oc.dot(oc) - radius * radius
        val discriminant = halfB * halfB - a * c

        if (discriminant < 0) return null

        val sqrtD = sqrt(discriminant)
        var root = (-halfB - sqrtD) / a
        if (root < tMin || root > tMax) {
            root = (-halfB + sqrtD) / a
            if (root < tMin || root > tMax) return null
        }

        val point = ray.pointAt(root)
        val outwardNormal = (point - center) / radius
        val frontFace = ray.direction.dot(outwardNormal) < 0
        val normal = if (frontFace) outwardNormal else outwardNormal * -1.0

        return HitRecord(point, normal, root, material, frontFace)
    }
}