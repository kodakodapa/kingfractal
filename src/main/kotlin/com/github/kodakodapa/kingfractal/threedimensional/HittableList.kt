package com.github.kodakodapa.kingfractal.threedimensional

class HittableList(val objects: MutableList<Hittable> = mutableListOf()) : Hittable {

    fun add(obj: Hittable) = objects.add(obj)

    fun clear() = objects.clear()

    override fun hit(ray: Ray, tMin: Double, tMax: Double): HitRecord? {
        var hitRecord: HitRecord? = null
        var closestSoFar = tMax

        for (obj in objects) {
            val tempRec = obj.hit(ray, tMin, closestSoFar)
            if (tempRec != null) {
                closestSoFar = tempRec.t
                hitRecord = tempRec
            }
        }

        return hitRecord
    }
}