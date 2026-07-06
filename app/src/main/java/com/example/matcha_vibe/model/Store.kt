package com.example.matcha_vibe.model

data class Store(
    var id: String = "",
    var name: String = "",
    var address: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0
) {
    override fun toString(): String {
        return "$name - $address"
    }
}
