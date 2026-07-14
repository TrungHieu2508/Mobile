package com.example.matcha_vibe

import kotlin.math.*

object LocationUtils {
    /**
     * Tính khoảng cách giữa 2 điểm tọa độ (Lat, Lng) theo công thức Haversine (đơn vị: km)
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Bán kính trái đất tính bằng km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
