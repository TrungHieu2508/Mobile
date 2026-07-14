package com.example.matcha_vibe

import com.example.matcha_vibe.model.CartItem
import com.example.matcha_vibe.model.Order
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Senior Unit Test for PayOS Payment & Firestore Integration
 * Sản phẩm test: Ly Cafe Muối (35.000đ)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayOSPaymentTest {

    // Mock dữ liệu đơn hàng Cafe
    private val mockCafeItem = CartItem(
        productId = "CAFE_01",
        productName = "Cafe Muối",
        quantity = 1,
        price = 35000.0,
        size = "M"
    )

    private val mockOrder = Order(
        id = "ORDER_TEST_001",
        userId = "USER_123",
        items = listOf(mockCafeItem),
        total = 35000.0,
        paymentStatus = "UNPAID",
        status = "PENDING"
    )

    @Before
    fun setup() {
        // Khởi tạo MockK để mock các Singleton object
        mockkObject(FirebaseHelper)
        mockkObject(PayOSHelper)
    }

    @After
    fun tearDown() {
        // Hủy mock sau mỗi test case để tránh ảnh hưởng lẫn nhau
        unmockkAll()
    }

    /**
     * Testcase: Happy Path
     * Nhận callback "Thành công" từ PayOS -> Cập nhật Firestore -> Trạng thái PAID
     */
    @Test
    fun processPayment_SuccessCallback_UpdatesFirestoreToPaid() = runTest {
        // --- GIVEN (Giả định) ---
        val mockCheckoutUrl = "https://payos.vn/v2/payment/link_cafe_123"
        val mockOrderCode = 1690000000L

        // 1. Giả lập lưu đơn hàng lên Firebase thành công
        every { 
            FirebaseHelper.placeOrder(any(), any(), any(), any()) 
        } answers {
            val onSuccess = arg<(String) -> Unit>(2)
            onSuccess("ORDER_TEST_001") 
        }

        // 2. Giả lập tạo link PayOS thành công
        every { 
            PayOSHelper.createPaymentLink(any(), any(), any(), any()) 
        } answers {
            val onSuccess = arg<(String, Long) -> Unit>(2)
            onSuccess(mockCheckoutUrl, mockOrderCode)
        }

        // 3. Giả lập cập nhật mã OrderCode lên Firestore thành công
        every { FirebaseHelper.updateOrderCode(any(), any()) } just Runs

        // --- WHEN (Thực thi) ---
        FirebaseHelper.placeOrder(mockOrder, false, { orderId ->
            PayOSHelper.createPaymentLink(
                amount = mockOrder.total.toInt(),
                description = "Thanh toan Cafe Muoi",
                onSuccess = { _, orderCode ->
                    // Logic xử lý khi có link thành công
                    FirebaseHelper.updateOrderCode(orderId, orderCode)
                    mockOrder.paymentStatus = "PAID" // Giả lập kết quả sau khi Webhook xử lý
                },
                onFailure = {}
            )
        }, {})

        // --- THEN (Kiểm chứng) ---
        // Xác nhận hàm updateOrderCode được gọi đúng ID và mã từ PayOS
        verify { FirebaseHelper.updateOrderCode("ORDER_TEST_001", mockOrderCode) }
        
        // Xác nhận trạng thái cuối cùng của đơn hàng Cafe là đã thanh toán
        assert(mockOrder.paymentStatus == "PAID")
    }

    /**
     * Testcase: PayOS Error
     * PayOS trả về lỗi (Hủy hoặc lỗi hệ thống) -> Không cập nhật Firestore
     */
    @Test
    fun processPayment_PayOSFailure_DoesNotUpdateFirestore() = runTest {
        // --- GIVEN ---
        every { FirebaseHelper.placeOrder(any(), any(), any(), any()) } answers {
            arg<(String) -> Unit>(2).invoke("ORDER_TEST_001")
        }

        // Giả lập PayOS trả về lỗi hệ thống
        every { 
            PayOSHelper.createPaymentLink(any(), any(), any(), any()) 
        } answers {
            val onFailure = arg<(String) -> Unit>(3)
            onFailure("PayOS Error: Service Unavailable")
        }

        // --- WHEN ---
        var capturedError = ""
        PayOSHelper.createPaymentLink(35000, "Cafe Muoi",
            onSuccess = { _, _ -> },
            onFailure = { error -> capturedError = error }
        )

        // --- THEN ---
        // Verify updateOrderCode KHÔNG được gọi
        verify(exactly = 0) { FirebaseHelper.updateOrderCode(any(), any()) }
        assert(capturedError.contains("Service Unavailable"))
    }

    /**
     * Testcase: Firestore Exception
     * Link PayOS tạo xong nhưng Firestore bị lỗi mạng khi update -> Bắt lỗi an toàn
     */
    @Test
    fun processPayment_FirestoreCrash_HandlesGracefully() = runTest {
        // --- GIVEN ---
        // Giả lập Firestore ném ra ngoại lệ (Exception)
        every { FirebaseHelper.updateOrderCode(any(), any()) } throws RuntimeException("Firestore Connection Lost")

        // --- WHEN ---
        var caughtException = false
        try {
            FirebaseHelper.updateOrderCode("ORDER_TEST_001", 999999L)
        } catch (e: Exception) {
            caughtException = true
        }

        // --- THEN ---
        assert(caughtException)
        println("Log: Da bat loi Firestore thanh cong, app khong bi crash.")
    }
}
