package com.example.matcha_vibe

import com.example.matcha_vibe.model.Product
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit Test giả lập các sản phẩm thực tế có trong Database của Matcha Vibe
 * Kiểm tra logic trừ kho cho menu: Matcha Latte, Cafe Muối, Bánh Kem.
 */
class DatabaseProductsStockTest {

    // Danh sách sản phẩm "giả lập" giống trong database thật
    private val dbProducts = mutableMapOf(
        "PROD_MATCHA" to Product(id = "PROD_MATCHA", name = "Matcha Latte", stockQuantity = 50, price = 45000.0),
        "PROD_CAFE" to Product(id = "PROD_CAFE", name = "Cafe Muối", stockQuantity = 20, price = 35000.0),
        "PROD_CAKE" to Product(id = "PROD_CAKE", name = "Bánh Kem Matcha", stockQuantity = 5, price = 55000.0)
    )

    @Before
    fun setup() {
        mockkObject(FirebaseHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Logic nghiệp vụ: Trừ kho dựa trên ID sản phẩm
     */
    private fun processStockDeduction(productId: String, quantity: Int): Int {
        val product = dbProducts[productId] ?: throw Exception("Sản phẩm không tồn tại")
        
        if (product.stockQuantity < quantity) {
            throw Exception("Sản phẩm ${product.name} không đủ hàng (Còn: ${product.stockQuantity})")
        }
        
        // Cập nhật số lượng mới vào "Database giả lập"
        val newStock = product.stockQuantity - quantity
        dbProducts[productId] = product.copy(stockQuantity = newStock)
        
        return newStock
    }

    /**
     * Test Case 1: Khách mua Matcha Latte (Số lượng lớn)
     */
    @Test
    fun test_DeductMatchaLatte_Success() {
        // Given: Matcha Latte có 50 ly, khách mua 10 ly
        val productId = "PROD_MATCHA"
        val buyAmount = 10

        // When: Trừ kho
        val remaining = processStockDeduction(productId, buyAmount)

        // Then: Còn lại 40 ly
        assert(remaining == 40)
        assert(dbProducts[productId]?.stockQuantity == 40)
        println("Success: Matcha Latte con lai $remaining ly trong kho.")
    }

    /**
     * Test Case 2: Khách mua Bánh Kem Matcha (Vừa hết sạch)
     */
    @Test
    fun test_DeductMatchaCake_SoldOut() {
        // Given: Bánh Kem chỉ còn 5 cái, khách mua đúng 5 cái
        val productId = "PROD_CAKE"
        val buyAmount = 5

        // When: Trừ kho
        val remaining = processStockDeduction(productId, buyAmount)

        // Then: Kho về 0
        assert(remaining == 0)
        println("Success: Banh Kem Matcha da ban het sach (Ton kho: 0).")
    }

    /**
     * Test Case 3: Lỗi khi mua quá số lượng Cafe Muối hiện có
     */
    @Test
    fun test_DeductCafeMuoi_InsufficientError() {
        // Given: Cafe Muối còn 20 ly, nhưng khách đặt 25 ly
        val productId = "PROD_CAFE"
        val buyAmount = 25
        var errorMsg = ""

        // When: Cố gắng trừ kho
        try {
            processStockDeduction(productId, buyAmount)
        } catch (e: Exception) {
            errorMsg = e.message ?: ""
        }

        // Then: Phải bắn ra lỗi và số lượng cũ (20) không đổi
        assert(errorMsg.contains("không đủ hàng"))
        assert(dbProducts[productId]?.stockQuantity == 20)
        println("Error Caught: $errorMsg - Bao ve kho thanh cong.")
    }
}
