package com.example.matcha_vibe.model

data class Product(
    var id: String = "",
    var name: String = "",
    var category: String = "", // "Matcha", "Coffee"
    var description: String = "",
    var price: Double = 0.0,
    var imageUrl: String = "",
    var available: Boolean = true,
    var stockQuantity: Int = 0 // Số lượng hàng còn lại
)
