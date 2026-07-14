package com.example.matcha_vibe.model

data class CartItem(
    var productId: String = "",
    var productName: String = "",
    var productImageUrl: String = "",
    var quantity: Int = 1,
    var price: Double = 0.0, // Đơn giá tại thời điểm thêm vào giỏ (đã cộng giá size nếu có)
    var size: String = "M",  // "S", "M", "L"
    var sugar: String = "100%", // "0%", "30%", "50%", "70%", "100%"
    var ice: String = "100%",   // "0%", "30%", "50%", "70%", "100%"
    var note: String = ""
) {
    fun getTotalPrice(): Double {
        return price * quantity
    }
}
