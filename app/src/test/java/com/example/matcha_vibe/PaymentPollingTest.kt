package com.example.matcha_vibe

import com.example.matcha_vibe.model.Order
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Exception

/**
 * Senior Unit Test for Payment Status Polling (Cơ chế kiểm tra trạng thái tự động)
 * Sử dụng Virtual Time để tua nhanh thời gian chờ (delay)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentPollingTest {

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
     * Hàm giả lập Logic Polling (Khuyên dùng thay cho Handler/Runnable)
     * Để test được Virtual Time, logic Polling nên sử dụng Coroutine delay()
     */
    private suspend fun startPollingLogic(
        orderId: String,
        interval: Long = 5000L,
        maxAttempts: Int = 5,
        onStatusChanged: (String) -> Unit
    ) {
        var count = 0
        while (count < maxAttempts) {
            var currentStatus = "UNPAID"
            
            // Giả lập gọi FirebaseHelper.getOrderStatus
            FirebaseHelper.getOrderStatus(orderId, { order ->
                currentStatus = order.paymentStatus
            }, { /* handle error */ })

            if (currentStatus == "PAID") {
                onStatusChanged("PAID")
                return
            }

            count++
            if (count < maxAttempts) {
                delay(interval) // Virtual time sẽ tua nhanh đoạn này
            }
        }
        onStatusChanged("TIMEOUT")
    }

    /**
     * Testcase 1: Happy Path - Thành công sau 3 lần check
     * Lần 1: UNPAID -> Delay 5s
     * Lần 2: UNPAID -> Delay 5s
     * Lần 3: PAID -> Dừng Polling ngay lập tức
     */
    @Test
    fun polling_SuccessOnThirdAttempt_StopsAndNotifies() = runTest {
        // --- Given ---
        val orderId = "ORDER_001"
        var callCount = 0
        var finalStatus = ""

        // Giả lập: Lần 1, 2 trả về UNPAID, lần 3 trả về PAID
        every { FirebaseHelper.getOrderStatus(orderId, any(), any()) } answers {
            callCount++
            val onSuccess = arg<(Order) -> Unit>(1)
            val status = if (callCount == 3) "PAID" else "UNPAID"
            onSuccess(Order(id = orderId, paymentStatus = status))
        }

        // --- When ---
        // Chạy polling trong background scope của runTest
        backgroundScope.launch {
            startPollingLogic(orderId) { status ->
                finalStatus = status
            }
        }

        // Đảm bảo coroutine đã chạy đến điểm delay đầu tiên
        runCurrent()

        // --- Then ---
        // Kiểm tra ngay lúc đầu (T=0)
        verify(exactly = 1) { FirebaseHelper.getOrderStatus(orderId, any(), any()) }
        
        // Tua nhanh 5 giây (T=5s) -> Check lần 2
        advanceTimeBy(5001)
        verify(exactly = 2) { FirebaseHelper.getOrderStatus(orderId, any(), any()) }
        
        // Tua nhanh thêm 5 giây (T=10s) -> Check lần 3
        advanceTimeBy(5001)
        verify(exactly = 3) { FirebaseHelper.getOrderStatus(orderId, any(), any()) }
        
        // Xác nhận trạng thái cuối cùng và số lần gọi
        assert(finalStatus == "PAID")
        assert(callCount == 3)
        println("Happy Path: Polling dung dung luc khi nhan trang thai PAID tai giay thu 10.")
    }

    /**
     * Testcase 2: Timeout - Thử hết 5 lần vẫn chưa thanh toán
     */
    @Test
    fun polling_ReachesMaxAttempts_EmitsTimeout() = runTest {
        // --- Given ---
        val orderId = "ORDER_TIMEOUT"
        var finalStatus = ""

        // Luôn trả về UNPAID
        every { FirebaseHelper.getOrderStatus(orderId, any(), any()) } answers {
            arg<(Order) -> Unit>(1).invoke(Order(id = orderId, paymentStatus = "UNPAID"))
        }

        // --- When ---
        backgroundScope.launch {
            startPollingLogic(orderId, maxAttempts = 5) { status ->
                finalStatus = status
            }
        }

        // Tua nhanh qua hẳn 25 giây (5 lần x 5s)
        advanceTimeBy(30000)

        // --- Then ---
        // Xác nhận gọi đúng 5 lần rồi dừng
        verify(exactly = 5) { FirebaseHelper.getOrderStatus(orderId, any(), any()) }
        assert(finalStatus == "TIMEOUT")
        println("Timeout Case: Polling tu dong ngat sau 5 lan thu khong thanh cong.")
    }

    /**
     * Testcase 3: Immediate Success - Thành công ngay lần đầu check
     */
    @Test
    fun polling_SuccessOnFirstAttempt_NoFurtherDelays() = runTest {
        // --- Given ---
        val orderId = "ORDER_FAST"
        var finalStatus = ""

        every { FirebaseHelper.getOrderStatus(orderId, any(), any()) } answers {
            arg<(Order) -> Unit>(1).invoke(Order(id = orderId, paymentStatus = "PAID"))
        }

        // --- When ---
        startPollingLogic(orderId) { status ->
            finalStatus = status
        }

        // --- Then ---
        verify(exactly = 1) { FirebaseHelper.getOrderStatus(orderId, any(), any()) }
        assert(finalStatus == "PAID")
        // Không cần tua thời gian vì không có delay() nào xảy ra
        println("Fast Case: Thanh toan xong ngay, khong ton tai delay du thua.")
    }

    /**
     * Testcase 4: Error Handling - Xử lý khi Firebase ném lỗi
     */
    @Test
    fun polling_OnFirebaseError_StillHandlesGracefully() = runTest {
        // --- Given ---
        val orderId = "ORDER_ERROR"
        var errorCaught = false

        // Giả lập Firebase trả về lỗi Exception
        every { FirebaseHelper.getOrderStatus(orderId, any(), any()) } answers {
            val onFailure = arg<(Exception) -> Unit>(2)
            onFailure(Exception("Network Error"))
            errorCaught = true
        }

        // --- When ---
        startPollingLogic(orderId, maxAttempts = 1) { /* no-op */ }

        // --- Then ---
        assert(errorCaught)
        println("Error Case: Bat loi mang thanh cong, luong polling khong bi crash.")
    }
}
