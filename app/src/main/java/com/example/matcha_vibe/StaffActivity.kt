package com.example.matcha_vibe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Order
import com.example.matcha_vibe.model.Product

class StaffActivity : AppCompatActivity() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var rvStock: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var stockAdapter: AdminProductAdapter // Tái sử dụng adapter admin nhưng chỉnh logic
    
    private lateinit var txtNoOrders: TextView
    private lateinit var btnTabPending: MaterialButton
    private lateinit var btnTabActive: MaterialButton
    private lateinit var btnTabStock: MaterialButton

    private var allOrdersList = listOf<Order>()
    private var filteredList = listOf<Order>()
    private var activeTab = "PENDING" // "PENDING", "ACTIVE", "STOCK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        rvOrders = findViewById(R.id.rvStaffOrders)
        rvStock = findViewById(R.id.rvStaffStock)
        txtNoOrders = findViewById(R.id.txtStaffNoOrders)
        btnTabPending = findViewById(R.id.btnTabPending)
        btnTabActive = findViewById(R.id.btnTabActive)
        btnTabStock = findViewById(R.id.btnTabStock)
        val btnBackToClient = findViewById<View>(R.id.btnStaffBackToClient)

        // Setup Orders Recycler
        rvOrders.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(
            orders = emptyList(),
            showActions = true,
            onNextAction = { order ->
                progressOrderState(order)
            },
            onCancelAction = { order ->
                cancelOrder(order)
            }
        )
        rvOrders.adapter = orderAdapter

        // Setup Stock Recycler
        rvStock.layoutManager = LinearLayoutManager(this)
        stockAdapter = AdminProductAdapter(
            products = emptyList(),
            onEditClick = { product ->
                showUpdateStockDialog(product)
            },
            onDeleteClick = {} // Nhân viên không có quyền xóa
        )
        rvStock.adapter = stockAdapter

        // Lấy đơn hàng realtime
        listenToOrdersRealtime()
        loadProductsStock()

        // Tab click
        btnTabPending.setOnClickListener {
            activeTab = "PENDING"
            filterOrders()
            highlightTab(btnTabPending, btnTabActive, btnTabStock)
            rvOrders.visibility = View.VISIBLE
            rvStock.visibility = View.GONE
        }

        btnTabActive.setOnClickListener {
            activeTab = "ACTIVE"
            filterOrders()
            highlightTab(btnTabActive, btnTabPending, btnTabStock)
            rvOrders.visibility = View.VISIBLE
            rvStock.visibility = View.GONE
        }

        btnTabStock.setOnClickListener {
            activeTab = "STOCK"
            highlightTab(btnTabStock, btnTabPending, btnTabActive)
            rvOrders.visibility = View.GONE
            rvStock.visibility = View.VISIBLE
            txtNoOrders.visibility = View.GONE
            loadProductsStock()
        }

        btnBackToClient.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun listenToOrdersRealtime() {
        FirebaseHelper.getAllOrdersRealtime(
            onUpdate = { list ->
                // Chỉ hiển thị các đơn hàng hợp lệ (Tiền mặt hoặc đã thanh toán QR thành công)
                allOrdersList = list.filter { 
                    it.paymentMethod == "CASH" || it.paymentStatus == "PAID"
                }
                filterOrders()
            },
            onFailure = { e ->
                Toast.makeText(this, "Lỗi kết nối thời gian thực: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun filterOrders() {
        filteredList = if (activeTab == "PENDING") {
            allOrdersList.filter { it.status == "PENDING" }
        } else {
            allOrdersList.filter { it.status == "PREPARING" || it.status == "DELIVERING" }
        }

        orderAdapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            txtNoOrders.visibility = View.VISIBLE
            rvOrders.visibility = View.GONE
        } else {
            txtNoOrders.visibility = View.GONE
            rvOrders.visibility = View.VISIBLE
        }
    }

    private fun loadProductsStock() {
        FirebaseHelper.getProducts(
            onSuccess = { list ->
                stockAdapter.updateData(list)
            },
            onFailure = { e ->
                Toast.makeText(this, "Lỗi tải kho: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showUpdateStockDialog(product: Product) {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(product.stockQuantity.toString())
        input.setPadding(50, 40, 50, 40)

        AlertDialog.Builder(this)
            .setTitle("Cập nhật kho: ${product.name}")
            .setMessage("Nhập số lượng hàng còn lại:")
            .setView(input)
            .setPositiveButton("Cập nhật") { _, _ ->
                val newStock = input.text.toString().toIntOrNull() ?: 0
                val updatedProduct = product.copy(stockQuantity = newStock)
                FirebaseHelper.updateProduct(updatedProduct,
                    onSuccess = {
                        Toast.makeText(this, "Đã cập nhật kho cho ${product.name}!", Toast.LENGTH_SHORT).show()
                        loadProductsStock()
                    },
                    onFailure = { e ->
                        Toast.makeText(this, "Lỗi cập nhật: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun highlightTab(active: MaterialButton, vararg inactives: MaterialButton) {
        active.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primaryGreen)
        active.setTextColor(ContextCompat.getColor(this, R.color.white))
        active.strokeWidth = 0
        for (inactive in inactives) {
            inactive.backgroundTintList = ContextCompat.getColorStateList(this, R.color.white)
            inactive.setTextColor(ContextCompat.getColor(this, R.color.primaryGreen))
            inactive.strokeColor = ContextCompat.getColorStateList(this, R.color.primaryGreen)
            inactive.strokeWidth = dpToPx(1)
        }
    }

    private fun progressOrderState(order: Order) {
        val nextStatus = when (order.status) {
            "PENDING" -> "PREPARING"
            "PREPARING" -> if (order.type == "DELIVERY") "DELIVERING" else "COMPLETED"
            "DELIVERING" -> "COMPLETED"
            else -> order.status
        }

        // Tự động chuyển trạng thái thanh toán khi hoàn thành đơn hoặc duyệt đơn
        val nextPaymentStatus = if (order.paymentMethod == "CASH" && nextStatus == "COMPLETED") {
            "PAID" // Nếu trả tiền mặt và làm xong/giao xong -> Đã thu tiền mặt -> PAID
        } else {
            order.paymentStatus
        }

        FirebaseHelper.updateOrderStatus(
            orderId = order.id,
            status = nextStatus,
            paymentStatus = nextPaymentStatus,
            onSuccess = {
                // Nếu đơn hàng đã thanh toán mà chưa trừ kho (ví dụ đơn QR vừa được nạp tiền)
                if (nextPaymentStatus == "PAID" && !order.stockDeducted) {
                    FirebaseHelper.deductStockForOrder(order.id, {
                        Toast.makeText(this, "Đã cập nhật trạng thái và trừ kho hàng!", Toast.LENGTH_SHORT).show()
                    }, { e ->
                        Toast.makeText(this, "Lỗi trừ kho: ${e.message}", Toast.LENGTH_LONG).show()
                    })
                } else {
                    Toast.makeText(this, "Đã cập nhật trạng thái đơn hàng!", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { e ->
                Toast.makeText(this, "Cập nhật thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun cancelOrder(order: Order) {
        FirebaseHelper.updateOrderStatus(
            orderId = order.id,
            status = "CANCELLED",
            paymentStatus = order.paymentStatus,
            onSuccess = {
                Toast.makeText(this, "Đã hủy đơn hàng!", Toast.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Toast.makeText(this, "Hủy đơn thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
