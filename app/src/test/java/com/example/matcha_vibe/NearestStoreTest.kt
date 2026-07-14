package com.example.matcha_vibe

import com.example.matcha_vibe.model.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearestStoreTest {

    @Test
    fun getNearestStore_compareTwoLocations_returnsClosest() {
        val userLat = 10.7769
        val userLng = 106.7009

        val storeA = Store(name = "Matcha Vibe Quận 1", lat = 10.7780, lng = 106.7020)
        val storeB = Store(name = "Matcha Vibe Quận 7", lat = 10.7280, lng = 106.7200)

        val distA = LocationUtils.calculateDistance(userLat, userLng, storeA.lat, storeA.lng)
        val distB = LocationUtils.calculateDistance(userLat, userLng, storeB.lat, storeB.lng)

        assertTrue("Cửa hàng A ($distA km) phải gần hơn B ($distB km)", distA < distB)

        val nearestStore = listOf(storeA, storeB).minByOrNull {
            LocationUtils.calculateDistance(userLat, userLng, it.lat, it.lng)
        }
        assertEquals("Matcha Vibe Quận 1", nearestStore?.name)
    }

    @Test
    fun calculateDistance_identicalCoordinates_returnsZero() {
        // Test 3: Khách đứng ngay tại tọa độ Store
        val lat = 10.8000
        val lng = 106.8000
        
        val distance = LocationUtils.calculateDistance(lat, lng, lat, lng)
        
        assertEquals(0.0, distance, 0.001)
        println("Test thành công: Khoảng cách bằng 0 khi tọa độ trùng nhau.")
    }

    @Test
    fun filterStores_withinRadius_returnsOnlyNearby() {
        val userLat = 10.7769
        val userLng = 106.7009
        
        val stores = listOf(
            Store(name = "Gần", lat = 10.7780, lng = 106.7020), // ~0.2km
            Store(name = "Xa", lat = 10.8500, lng = 106.9000)   // ~20km
        )
        
        // Test 4: Chỉ lấy những cửa hàng trong bán kính 2km
        val nearbyStores = stores.filter { 
            LocationUtils.calculateDistance(userLat, userLng, it.lat, it.lng) <= 2.0 
        }
        
        assertEquals(1, nearbyStores.size)
        assertEquals("Gần", nearbyStores[0].name)
        println("Test thành công: Đã lọc đúng các cửa hàng trong bán kính 2km.")
    }
}
