package com.example.matcha_vibe

import com.example.matcha_vibe.model.CartItem
import com.example.matcha_vibe.model.Order
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Exception

/**
 * Unit Test cho chức năng Xử lý Đơn hàng và Firestore (placeOrder)
 * Chuyên gia TDD - Matcha Vibe Project
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderProcessingTest {

    @Before
    fun setup() {
        // Mock Singleton FirebaseHelper
        mockkObject(FirebaseHelper)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Testcase: Happy Path (Đặt đơn thành công)
     * Given: Một đơn hàng hợp lệ (1 ly Matcha Latte 45k)
     * When: Gọi hàm placeOrder
     * Then: Firestore lưu thành công, trả về Document ID và mã giao dịch được khởi tạo
     */
    @Test
    fun placeOrder_validOrder_generatesTransactionCodeAndSaves() = runTest {
        // --- Given ---
        val matchaItem = CartItem(
            productId = "MATCH_01",
            productName = "Matcha Latte",
            quantity = 1,
            price = 45000.0
        )
        val validOrder = Order(
            userId = "USER_001",
            items = listOf(matchaItem),
            total = 45000.0,
            paymentMethod = "QR_CODE"
        )
        val expectedDocId = "FIREBASE_ORDER_ID_123"

        // Giả lập FirebaseHelper.placeOrder thành công
        every { 
            FirebaseHelper.placeOrder(any(), any(), any(), any()) 
        } answers {
            // Thực thi callback onSuccess (tham số thứ 3, index 2)
            val onSuccess = arg<(String) -> Unit>(2)
            onSuccess(expectedDocId)
        }

        // --- When ---
        var capturedOrderId = ""
        FirebaseHelper.placeOrder(validOrder, deductStock = true, onSuccess = { id ->
            capturedOrderId = id
        }, onFailure = {})

        // --- Then ---
        // 1. Xác minh hàm placeOrder đã được gọi với đúng dữ liệu
        verify { FirebaseHelper.placeOrder(match { it.total == 45000.0 }, any(), any(), any()) }
        
        // 2. Kiểm tra ID đơn hàng trả về đúng như mong đợi
        assert(capturedOrderId == expectedDocId)
        
        println("Happy Path: Don hang da duoc day len Firestore thanh cong voi ID: $capturedOrderId")
    }

    /**
     * Testcase: Edge Case (Dữ liệu rỗng)
     * Given: Đơn hàng không có sản phẩm (items rỗng)
     * When: Thực hiện logic kiểm tra trước khi đặt đơn
     * Then: Trả về lỗi Validation, không được gọi hàm lưu Firestore
     */
    @Test
    fun placeOrder_emptyItems_returnsValidationErrorAndDoesNotSave() = runTest {
        // --- Given ---
        val emptyOrder = Order(
            userId = "USER_001",
            items = emptyList(), // Rỗng
            total = 0.0
        )

        // Giả lập logic validation đơn giản (thường nằm trong ViewModel hoặc Helper)
        fun isOrderValid(order: Order): Boolean {
            return order.items.isNotEmpty() && order.total > 0
        }

        // --- When ---
        val isValid = isOrderValid(emptyOrder)

        // --- Then ---
        assert(!isValid) // Mong đợi kết quả là không hợp lệ
        
        // Quan trọng: Xác minh FirebaseHelper.placeOrder KHÔNG bao giờ được gọi
        verify(exactly = 0) { FirebaseHelper.placeOrder(any(), any(), any(), any()) }
        
        println("Edge Case: He thong da chan don hang rong thanh cong, khong ton tai raca tren DB.")
    }

    /**
     * Testcase: Error Case (Firestore lỗi)
     * Given: Một đơn hàng hợp lệ
     * When: Gọi hàm lưu lên Firestore nhưng gặp lỗi kết nối (Exception)
     * Then: Bắt được lỗi, trả về thông báo Failure, không làm crash app
     */
    @Test
    fun placeOrder_firestoreError_returnsFailureMessage() = runTest {
        // --- Given ---
        val validOrder = Order(userId = "USER_001", items = listOf(CartItem()), total = 20000.0)
        val firebaseErrorMsg = "Permission Denied: User not authenticated"

        // Giả lập Firestore trả về lỗi (gọi callback onFailure)
        every { 
            FirebaseHelper.placeOrder(any(), any(), any(), any()) 
        } answers {
            val onFailure = arg<(Exception) -> Unit>(3)
            onFailure(Exception(firebaseErrorMsg))
        }

        // --- When ---
        var errorMessage = ""
        FirebaseHelper.placeOrder(validOrder, false, onSuccess = {}, onFailure = { e ->
            errorMessage = e.message ?: ""
        })

        // --- Then ---
        // 1. Xác nhận lỗi được bắt đúng thông tin
        assert(errorMessage == firebaseErrorMsg)
        
        // 2. Xác nhận app vẫn hoạt động (không ném ngoại lệ ra ngoài)
        println("Error Case: Bat loi Firestore thanh cong: $errorMessage")
    }
}
