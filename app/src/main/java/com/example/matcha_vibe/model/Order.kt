package com.example.matcha_vibe.model

data class Order(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var userPhone: String = "",
    var type: String = "DELIVERY", // "DELIVERY" hoặc "DINE_IN"
    var storeId: String = "",
    var storeAddress: String = "",
    var tableNumber: String = "",  // Chỉ dùng cho DINE_IN
    var address: String = "",      // Dùng cho DELIVERY (địa chỉ nhà) hoặc DINE_IN (tên quán - số bàn)
    var items: List<CartItem> = emptyList(),
    var subtotal: Double = 0.0,
    var discountCode: String = "",
    var discountAmount: Double = 0.0,
    var shippingFee: Double = 0.0,
    var total: Double = 0.0,
    var orderCode: Long = 0,               // Mã đơn hàng dạng số cho PayOS
    var paymentMethod: String = "QR_CODE", // "QR_CODE", "CASH"
    var paymentStatus: String = "UNPAID",   // "UNPAID", "PAID"
    var status: String = "PENDING", // "PENDING", "PREPARING", "DELIVERING"/"READY_AT_TABLE", "COMPLETED", "CANCELLED"
    var stockDeducted: Boolean = false,    // Đã trừ kho chưa
    var timestamp: Long = System.currentTimeMillis()
)
