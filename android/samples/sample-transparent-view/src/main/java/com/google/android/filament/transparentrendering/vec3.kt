package com.google.android.filament.transparentrendering

import kotlin.math.sqrt

class Vec3 (val e0:Float, val e1: Float, val e2: Float){

    val e = Array<Float>(3){ 0.0f }

    init{
        e[0] = e0
        e[1] = e1
        e[2] = e2
    }

    public fun x(): Float {
        return e[0]
    }
    public fun y(): Float {
        return e[1]
    }
    public fun z(): Float {
        return e[2]
    }
    public fun r(): Float {
        return e[0]
    }
    public fun g(): Float {
        return e[1]
    }
    public fun b(): Float {
        return e[2]
    }

    public fun length(): Float {
        return sqrt(e[0]*e[0] + e[1]*e[1] + e[2]*e[2])
    }

    public fun squared_length(): Float{
        return e[0]*e[0] + e[1]*e[1] + e[2]*e[2]
    }

    public fun make_unit_vector(){
        val k:Float = 1.0f / sqrt(e[0]*e[0] + e[1]*e[1] + e[2]*e[2]);
        e[0] *= k; e[1] *= k; e[2] *= k;
    }

    operator fun plus(vec2: Vec3): Vec3 {
        return Vec3(x() + vec2.x(), y() + vec2.y(), z() + vec2.z())
    }

    operator fun minus(vec2: Vec3): Vec3 {
        return Vec3(x() - vec2.x(), y() - vec2.y(), z() - vec2.z())
    }

    operator fun times(vec2: Vec3): Vec3 {
        return Vec3(x() * vec2.x(), y() * vec2.y(), z() * vec2.z())
    }

    operator fun div(vec2: Vec3): Vec3 {
        return Vec3(x() / vec2.x(), y() / vec2.y(), z() / vec2.z())
    }

    operator fun times(t: Float): Vec3 {
        return Vec3(x() * t, y() * t, z() * t)
    }

    operator fun div(t: Float): Vec3 {
        return Vec3(x() / t, y() / t, z() / t)
    }


}