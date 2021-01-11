package com.spqrta.camera2demo.utility.pure

import kotlin.math.pow

object LocationUtils {

    fun calcVisibilityRadius(zoom: Double): Double {
        return 1 / 2.0.pow(zoom + 1) * EQUATOR * 1000
    }

    const val EQUATOR = 40075.7
}