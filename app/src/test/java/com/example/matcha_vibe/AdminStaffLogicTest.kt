package com.example.matcha_vibe

import com.example.matcha_vibe.model.Order
import com.example.matcha_vibe.model.Promo
import com.example.matcha_vibe.model.User
import org.junit.Test
import java.util.*

/**
 * Unit Test cho các chức năng (Admin & Staff System)
 * Bao gồm: Điều phối đơn hàng, Quản lý người dùng, Doanh thu và Khuyến mãi.
 */
class AdminStaffLogicTest {

    /**
     * Test logic điều phối đơn hàng của Nhân viên (Staff)
     * Trạng thái: PENDING -> PREPARING -> DELIVERING/COMPLETED
     */
    @Test
    fun test_OrderStateProgression_Logic() {
        // Giả lập 1 đơn hàng tại quán (PICKUP/AT_TABLE)
        val orderPickup = Order(id = "ORDER_001", status = "PENDING", type = "PICKUP", paymentMethod = "CASH")
        
        // 1. Staff bấm Duyệt đơn -> Sang PREPARING
        val status2 = getNextStatus(orderPickup.status, orderPickup.type)
        assert(status2 == "PREPARING")

        // 2. Staff bấm Xong -> Sang COMPLETED (vì không phải giao hàng)
        val status3 = getNextStatus(status2, orderPickup.type)
        assert(status3 == "COMPLETED")

        // Giả lập 1 đơn hàng Giao hàng (DELIVERY)
        val orderDeliv = Order(id = "ORDER_002", status = "PREPARING", type = "DELIVERY")
        val status4 = getNextStatus(orderDeliv.status, orderDeliv.type)
        assert(status4 == "DELIVERING")
        
        val status5 = getNextStatus(status4, orderDeliv.type)
        assert(status5 == "COMPLETED")
        
        println("Success: Order progression logic verified for both Pickup and Delivery.")
    }

    /**
     * Test logic tự động cập nhật trạng thái thanh toán khi hoàn thành đơn Cash
     */
    @Test
    fun test_PaymentStatusAutoUpdate_Logic() {
        val order = Order(id = "ORD1", status = "DELIVERING", paymentMethod = "CASH", paymentStatus = "UNPAID")
        
        // Giả lập logic trong StaffActivity khi bấm hoàn thành đơn
        val nextStatus = getNextStatus(order.status, "DELIVERY") 
        val nextPaymentStatus = if (order.paymentMethod == "CASH" && nextStatus == "COMPLETED") {
            "PAID"
        } else {
            order.paymentStatus
        }

        assert(nextStatus == "COMPLETED")
        assert(nextPaymentStatus == "PAID")
        println("Success: Cash order automatically marked as PAID upon completion.")
    }

    /**
     * Test logic lọc người dùng theo vai trò (Admin User Management)
     */
    @Test
    fun test_UserRoleFiltering_Logic() {
        val users = listOf(
            User(uid = "1", name = "Admin Bao", role = "ADMIN"),
            User(uid = "2", name = "Staff Son", role = "STAFF"),
            User(uid = "3", name = "Staff Tuan", role = "STAFF"),
            User(uid = "4", name = "Customer An", role = "CUSTOMER")
        )

        val staffList = users.filter { it.role == "STAFF" }
        assert(staffList.size == 2)
        assert(staffList.any { it.name == "Staff Son" })

        val adminList = users.filter { it.role == "ADMIN" }
        assert(adminList.size == 1)
        
        println("Success: User filtering by role works correctly.")
    }

    /**
     * Test logic tính toán doanh thu (Admin Dashboard)
     */
    @Test
    fun test_RevenueCalculation_Logic() {
        val orders = listOf(
            Order(id = "O1", total = 50000.0, status = "COMPLETED", paymentStatus = "PAID"),
            Order(id = "O2", total = 100000.0, status = "COMPLETED", paymentStatus = "PAID"),
            Order(id = "O3", total = 30000.0, status = "CANCELLED", paymentStatus = "UNPAID"),
            Order(id = "O4", total = 20000.0, status = "PENDING", paymentStatus = "PAID") // Đã thanh toán nhưng chưa xong
        )

        // Admin chỉ tính doanh thu từ các đơn COMPLETED
        val totalRevenue = orders.filter { it.status == "COMPLETED" }.sumOf { it.total }
        
        assert(totalRevenue == 150000.0)
        println("Success: Revenue calculation only includes COMPLETED orders.")
    }

    /**
     * Test logic áp dụng mã giảm giá (Promo Logic)
     */
    @Test
    fun test_PromoDiscount_Logic() {
        val promo = Promo(code = "MATCHA10", discountPercent = 10, minOrderValue = 100000.0)
        val orderValue = 150000.0
        
        var finalPrice = orderValue
        if (orderValue >= promo.minOrderValue) {
            val discount = orderValue * promo.discountPercent / 100
            finalPrice = orderValue - discount
        }

        assert(finalPrice == 135000.0)
        
        // Test trường hợp không đủ giá trị tối thiểu
        val smallOrder = 50000.0
        var finalSmallPrice = smallOrder
        if (smallOrder >= promo.minOrderValue) {
            finalSmallPrice -= (smallOrder * promo.discountPercent / 100)
        }
        assert(finalSmallPrice == 50000.0)
        
        println("Success: Promo discount logic verified.")
    }

    // --- Hàm Helper mô phỏng logic trong StaffActivity ---
    private fun getNextStatus(currentStatus: String, type: String): String {
        return when (currentStatus) {
            "PENDING" -> "PREPARING"
            "PREPARING" -> if (type == "DELIVERY") "DELIVERING" else "COMPLETED"
            "DELIVERING" -> "COMPLETED"
            else -> currentStatus
        }
    }
}
