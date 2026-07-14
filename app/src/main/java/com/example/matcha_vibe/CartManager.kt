package com.example.matcha_vibe

import com.example.matcha_vibe.model.CartItem
import com.example.matcha_vibe.model.Product
import com.example.matcha_vibe.model.Promo

object CartManager {
    val cartList = mutableListOf<CartItem>()
    var appliedPromo: Promo? = null
    var shippingFee: Double = 0.0

    interface CartListener {
        fun onCartChanged()
    }

    private val listeners = mutableListOf<CartListener>()

    fun addListener(listener: CartListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CartListener) {
        listeners.remove(listener)
    }

    fun notifyListeners() {
        listeners.forEach { it.onCartChanged() }
    }

    fun getCartCount(): Int {
        return cartList.sumOf { it.quantity }
    }

    fun addProduct(product: Product, size: String, sugar: String, ice: String, quantity: Int, note: String) {
        // Tính giá phụ thêm dựa trên kích cỡ
        val extraPrice = when (size) {
            "S" -> -5000.0 // Giảm 5k cho size nhỏ
            "L" -> 5000.0  // Thêm 5k cho size lớn
            else -> 0.0    // Size M giữ nguyên giá gốc
        }
        val finalUnitPrice = product.price + extraPrice

        // Kiểm tra xem sản phẩm cùng tùy chọn đã tồn tại chưa
        val existingItem = cartList.find {
            it.productId == product.id && it.size == size && it.sugar == sugar && it.ice == ice && it.note == note
        }

        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            cartList.add(
                CartItem(
                    productId = product.id,
                    productName = product.name,
                    productImageUrl = product.imageUrl,
                    quantity = quantity,
                    price = finalUnitPrice,
                    size = size,
                    sugar = sugar,
                    ice = ice,
                    note = note
                )
            )
        }
        notifyListeners()
    }

    fun removeProduct(index: Int) {
        if (index in cartList.indices) {
            cartList.removeAt(index)
            notifyListeners()
        }
    }

    fun updateQuantity(index: Int, quantity: Int) {
        if (index in cartList.indices) {
            if (quantity <= 0) {
                cartList.removeAt(index)
            } else {
                cartList[index].quantity = quantity
            }
            notifyListeners()
        }
    }

    fun clearCart() {
        cartList.clear()
        appliedPromo = null
        shippingFee = 0.0
        notifyListeners()
    }

    fun getSubtotal(): Double {
        return cartList.sumOf { it.getTotalPrice() }
    }

    fun getDiscountAmount(): Double {
        val promo = appliedPromo ?: return 0.0
        val subtotal = getSubtotal()
        if (subtotal >= promo.minOrderValue) {
            return (subtotal * promo.discountPercent / 100.0)
        }
        return 0.0
    }

    fun getTotal(): Double {
        val total = getSubtotal() - getDiscountAmount() + shippingFee
        return if (total < 0) 0.0 else total
    }
}
