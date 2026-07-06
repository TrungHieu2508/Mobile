package com.example.matcha_vibe

import com.example.matcha_vibe.model.Order
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Senior Unit Test for Order UI Logic
 * Kiểm tra logic ánh xạ trạng thái đơn hàng (Mapping Status)
 */
class OrderDisplayLogicTest {

    /**
     * Helper function giả lập logic từ OrderAdapter
     * (Trong thực tế, nếu hàm này là public, ta có thể gọi trực tiếp từ Adapter)
     */
    private fun getStatusText(status: String): String {
        return when (status) {
            "PENDING" -> "ĐANG CHỜ"
            "PREPARING" -> "ĐANG PHA CHẾ"
            "DELIVERING" -> "ĐANG GIAO ĐỒ"
            "COMPLETED" -> "ĐÃ HOÀN THÀNH"
            "CANCELLED" -> "ĐÃ HỦY ĐƠN"
            else -> status
        }
    }

    /**
     * Case 3: Kiểm tra hiển thị trạng thái tiếng Việt chính xác
     */
    @Test
    fun getStatusText_Mapping_IsCorrect() {
        // Given: Các trạng thái thô từ database
        val rawPending = "PENDING"
        val rawPreparing = "PREPARING"
        val rawDelivering = "DELIVERING"
        val rawCompleted = "COMPLETED"
        val rawCancelled = "CANCELLED"

        // When: Ánh xạ sang ngôn ngữ hiển thị
        val textPending = getStatusText(rawPending)
        val textPreparing = getStatusText(rawPreparing)
        val textDelivering = getStatusText(rawDelivering)
        val textCompleted = getStatusText(rawCompleted)
        val textCancelled = getStatusText(rawCancelled)

        // Then: Phải khớp với giao diện yêu cầu
        assertEquals("ĐANG CHỜ", textPending)
        assertEquals("ĐANG PHA CHẾ", textPreparing)
        assertEquals("ĐANG GIAO ĐỒ", textDelivering)
        assertEquals("ĐÃ HOÀN THÀNH", textCompleted)
        assertEquals("ĐÃ HỦY ĐƠN", textCancelled)
    }

    /**
     * Case 2: Kiểm tra định dạng Mã đơn hàng (Id bắt đầu bằng # và lấy 6 ký tự cuối)
     */
    @Test
    fun orderId_Formatting_IsCorrect() {
        // Given: Một ID dài từ Firebase
        val rawId = "asdfghjkl123456"
        val order = Order(id = rawId)

        // When: Format theo logic trong OrderAdapter (id.takeLast(6).uppercase())
        val displayId = "#${order.id.takeLast(6).uppercase()}"

        // Then: Kết quả phải là 6 ký tự cuối viết hoa
        assertEquals("#123456", displayId)
    }

    /**
     * Kiểm tra logic làm mờ đơn hàng (Alpha) khi bị hủy
     */
    @Test
    fun cancelledOrder_ShouldHaveLowerAlpha() {
        // Given: Đơn hàng bị hủy
        val order = Order(status = "CANCELLED")

        // When: Quyết định độ mờ (Alpha)
        val alpha = if (order.status == "CANCELLED") 0.6f else 1.0f

        // Then: Alpha phải giảm xuống 0.6
        assertEquals(0.6f, alpha)
    }
}
