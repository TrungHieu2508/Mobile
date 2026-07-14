package com.example.matcha_vibe

import com.example.matcha_vibe.model.Product
import com.example.matcha_vibe.model.Promo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho Cart Engine của Customer.
 * File này test trực tiếp object CartManager.kt và model CartItem.kt.
 */
class CustomerCartManagerTest {

    private val matchaLatte = Product(
        id = "P_MATCHA_LATTE",
        name = "Matcha Latte",
        category = "Matcha",
        price = 45000.0,
        imageUrl = "https://example.com/matcha.png",
        available = true,
        stockQuantity = 100
    )

    private val cafeSua = Product(
        id = "P_CAFE_SUA",
        name = "Cafe Sữa",
        category = "Coffee",
        price = 35000.0,
        imageUrl = "https://example.com/cafe.png",
        available = true,
        stockQuantity = 100
    )

    @Before
    fun setUp() {
        CartManager.clearCart()
    }

    @After
    fun tearDown() {
        CartManager.clearCart()
    }

    @Test
    fun addProduct_sizeM_addsNewCartItemWithBasePrice() {
        CartManager.addProduct(
            product = matchaLatte,
            size = "M",
            sugar = "70%",
            ice = "50%",
            quantity = 1,
            note = "ít ngọt"
        )

        assertEquals(1, CartManager.cartList.size)
        assertEquals(1, CartManager.getCartCount())
        assertEquals("P_MATCHA_LATTE", CartManager.cartList[0].productId)
        assertEquals("Matcha Latte", CartManager.cartList[0].productName)
        assertEquals(45000.0, CartManager.cartList[0].price, 0.001)
        assertEquals(45000.0, CartManager.getSubtotal(), 0.001)
    }

    @Test
    fun addProduct_sizeSAndL_appliesExtraPriceCorrectly() {
        CartManager.addProduct(matchaLatte, "S", "70%", "50%", 1, "")
        CartManager.addProduct(matchaLatte, "L", "70%", "50%", 1, "")

        assertEquals(2, CartManager.cartList.size)
        assertEquals(40000.0, CartManager.cartList[0].price, 0.001) // S = base - 5.000
        assertEquals(50000.0, CartManager.cartList[1].price, 0.001) // L = base + 5.000
        assertEquals(90000.0, CartManager.getSubtotal(), 0.001)
    }

    @Test
    fun addProduct_sameProductAndSameOptions_mergesQuantity() {
        CartManager.addProduct(matchaLatte, "M", "50%", "50%", 1, "không topping")
        CartManager.addProduct(matchaLatte, "M", "50%", "50%", 2, "không topping")

        assertEquals(1, CartManager.cartList.size)
        assertEquals(3, CartManager.cartList[0].quantity)
        assertEquals(3, CartManager.getCartCount())
        assertEquals(135000.0, CartManager.getSubtotal(), 0.001)
    }

    @Test
    fun addProduct_sameProductButDifferentOptions_keepsSeparateCartLines() {
        CartManager.addProduct(matchaLatte, "M", "50%", "50%", 1, "")
        CartManager.addProduct(matchaLatte, "M", "70%", "50%", 1, "")
        CartManager.addProduct(matchaLatte, "M", "50%", "50%", 1, "thêm đá")

        assertEquals(3, CartManager.cartList.size)
        assertNotEquals(CartManager.cartList[0].sugar, CartManager.cartList[1].sugar)
        assertNotEquals(CartManager.cartList[0].note, CartManager.cartList[2].note)
    }

    @Test
    fun updateQuantity_positiveQuantity_updatesCartLine() {
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "")

        CartManager.updateQuantity(index = 0, quantity = 4)

        assertEquals(4, CartManager.cartList[0].quantity)
        assertEquals(4, CartManager.getCartCount())
        assertEquals(140000.0, CartManager.getSubtotal(), 0.001)
    }

    @Test
    fun updateQuantity_zeroOrNegative_removesCartLine() {
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "")

        CartManager.updateQuantity(index = 0, quantity = 0)

        assertEquals(0, CartManager.cartList.size)
        assertEquals(0, CartManager.getCartCount())
    }

    @Test
    fun removeProduct_validIndex_removesOnlySelectedItem() {
        CartManager.addProduct(matchaLatte, "M", "70%", "50%", 1, "")
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "")

        CartManager.removeProduct(index = 0)

        assertEquals(1, CartManager.cartList.size)
        assertEquals("P_CAFE_SUA", CartManager.cartList[0].productId)
    }

    @Test
    fun removeProduct_invalidIndex_doesNothing() {
        CartManager.addProduct(matchaLatte, "M", "70%", "50%", 1, "")

        CartManager.removeProduct(index = 99)

        assertEquals(1, CartManager.cartList.size)
        assertEquals("P_MATCHA_LATTE", CartManager.cartList[0].productId)
    }

    @Test
    fun promoQualified_getDiscountAndTotal_areCorrect() {
        CartManager.addProduct(matchaLatte, "M", "70%", "50%", 2, "") // 90.000
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "") // 35.000
        CartManager.appliedPromo = Promo(
            code = "MATCHA10",
            discountPercent = 10,
            minOrderValue = 100000.0,
            active = true
        )
        CartManager.shippingFee = 15000.0

        assertEquals(125000.0, CartManager.getSubtotal(), 0.001)
        assertEquals(12500.0, CartManager.getDiscountAmount(), 0.001)
        assertEquals(127500.0, CartManager.getTotal(), 0.001)
    }

    @Test
    fun promoBelowMinOrderValue_discountIsZero() {
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "") // 35.000
        CartManager.appliedPromo = Promo(
            code = "MATCHA10",
            discountPercent = 10,
            minOrderValue = 100000.0,
            active = true
        )

        assertEquals(35000.0, CartManager.getSubtotal(), 0.001)
        assertEquals(0.0, CartManager.getDiscountAmount(), 0.001)
        assertEquals(35000.0, CartManager.getTotal(), 0.001)
    }

    @Test
    fun getTotal_discountGreaterThanSubtotal_neverReturnsNegative() {
        CartManager.addProduct(cafeSua, "M", "100%", "100%", 1, "")
        CartManager.appliedPromo = Promo(
            code = "OVER_DISCOUNT",
            discountPercent = 200,
            minOrderValue = 0.0,
            active = true
        )
        CartManager.shippingFee = 0.0

        assertEquals(0.0, CartManager.getTotal(), 0.001)
    }

    @Test
    fun clearCart_resetsItemsPromoAndShippingFee() {
        val promo = Promo("MATCHA10", 10, 100000.0, true)
        CartManager.addProduct(matchaLatte, "M", "70%", "50%", 1, "")
        CartManager.appliedPromo = promo
        CartManager.shippingFee = 12000.0

        CartManager.clearCart()

        assertEquals(0, CartManager.cartList.size)
        assertNull(CartManager.appliedPromo)
        assertEquals(0.0, CartManager.shippingFee, 0.001)
        assertEquals(0.0, CartManager.getTotal(), 0.001)
    }

    @Test
    fun cartListener_isNotifiedWhenCartChanges() {
        var notifyCount = 0
        val listener = object : CartManager.CartListener {
            override fun onCartChanged() {
                notifyCount++
            }
        }

        CartManager.addListener(listener)
        try {
            CartManager.addProduct(matchaLatte, "M", "70%", "50%", 1, "")
            CartManager.updateQuantity(0, 2)
            CartManager.removeProduct(0)
            CartManager.clearCart()

            assertEquals(4, notifyCount)
        } finally {
            CartManager.removeListener(listener)
        }
    }
}
