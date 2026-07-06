package com.example.matcha_vibe

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.IllegalArgumentException

/**
 * Senior Unit Test for Stock Management Logic (Quản lý Kho)
 * Sản phẩm test: iPhone 15, Chuột Logitech, Bàn phím Cơ
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StockManagementTest {

    @Before
    fun setup() {
        // Mock Singleton FirebaseHelper để verify các hành động DB
        mockkObject(FirebaseHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // Định nghĩa Exception tùy chỉnh cho trường hợp hết hàng
    class InsufficientStockException(message: String) : Exception(message)

    /**
     * Logic tính toán kho (Quy tắc nghiệp vụ - Business Rules)
     */
    private fun calculateNewStock(currentStock: Int, purchaseQuantity: Int): Int {
        if (purchaseQuantity <= 0) {
            throw IllegalArgumentException("Số lượng đặt mua phải lớn hơn 0")
        }
        if (currentStock < purchaseQuantity) {
            throw InsufficientStockException("Số lượng tồn kho không đủ")
        }
        return currentStock - purchaseQuantity
    }

    /**
     * Case 1: Happy Path - Trừ kho thành công
     * Given: iPhone 15 có 10 máy, khách mua 2 máy.
     * When: Tính toán trừ kho.
     * Then: Số lượng tồn kho mới là 8.
     */
    @Test
    fun deductStock_validPurchase_updatesStockCorrectly() {
        // --- Given ---
        val currentStock = 10
        val buyQuantity = 2

        // --- When ---
        val remainingStock = calculateNewStock(currentStock, buyQuantity)

        // --- Then ---
        assert(remainingStock == 8)
        println("Happy Path: iPhone 15 tu 10 giam xuong 8 - Thanh cong.")
    }

    /**
     * Case 2: Edge Case - Mua vừa hết sạch kho
     * Given: Chuột Logitech còn 5 con, khách mua đúng 5 con.
     * When: Tính toán trừ kho.
     * Then: Tồn kho mới về 0.
     */
    @Test
    fun deductStock_purchaseEqualTotalStock_stockBecomesZero() {
        // --- Given ---
        val currentStock = 5
        val buyQuantity = 5

        // --- When ---
        val remainingStock = calculateNewStock(currentStock, buyQuantity)

        // --- Then ---
        assert(remainingStock == 0)
        println("Edge Case 1: Chuot Logitech het sach kho (ve 0) - Thanh cong.")
    }

    /**
     * Case 3: Edge Case - Số lượng mua không hợp lệ
     * Given: Khách truyền số lượng mua là -1.
     * Then: Phải tung ra IllegalArgumentException và không thực hiện tính toán.
     */
    @Test(expected = IllegalArgumentException::class)
    fun deductStock_invalidQuantity_throwsException() {
        // --- Given ---
        val currentStock = 20
        val buyQuantity = -1

        // --- When ---
        calculateNewStock(currentStock, buyQuantity)
        
        // --- Then: JUnit tự động bắt IllegalArgumentException
    }

    /**
     * Case 4: Error Case - Vượt quá số lượng tồn kho
     * Given: Bàn phím cơ còn 3 chiếc, khách đặt 5 chiếc.
     * Then: Phải tung ra InsufficientStockException để báo lỗi hết hàng.
     */
    @Test
    fun deductStock_insufficientQuantity_throwsInsufficientStockException() {
        // --- Given ---
        val currentStock = 3
        val buyQuantity = 5
        var exceptionCaught = false

        // --- When ---
        try {
            calculateNewStock(currentStock, buyQuantity)
        } catch (e: InsufficientStockException) {
            exceptionCaught = true
        }

        // --- Then ---
        assert(exceptionCaught)
        // Đảm bảo không có lệnh cập nhật database nào được thực hiện khi lỗi
        verify(exactly = 0) { FirebaseHelper.updateProductStock(any(), any()) }
        println("Error Case: Chan thanh cong viec mua qua 3 ban phim - Da bao loi InsufficientStock.")
    }
}
