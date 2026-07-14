package com.example.matcha_vibe.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.FirebaseHelper
import com.example.matcha_vibe.OrderAdapter
import com.example.matcha_vibe.R
import com.example.matcha_vibe.model.Order

class OrdersFragment : Fragment() {

    private lateinit var rvOrders: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var txtNoOrders: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvOrders = view.findViewById(R.id.rvOrders)
        txtNoOrders = view.findViewById(R.id.txtNoOrders)

        rvOrders.layoutManager = LinearLayoutManager(requireContext())
        orderAdapter = OrderAdapter(
            orders = emptyList(),
            showActions = true, // Cho phép hiện nút Hủy
            onCancelAction = { order ->
                showCancelOrderDialog(order)
            }
        )
        rvOrders.adapter = orderAdapter

        loadUserOrders()
    }

    private fun showCancelOrderDialog(order: Order) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Hủy đơn hàng")
            .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
            .setPositiveButton("Hủy đơn") { _, _ ->
                FirebaseHelper.updateOrderStatus(order.id, "CANCELLED", order.paymentStatus,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Đã hủy đơn hàng thành công!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Lỗi khi hủy đơn: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Quay lại", null)
            .show()
    }

    private fun loadUserOrders() {
        val currentUid = FirebaseHelper.getCurrentUserId()
        if (currentUid != null) {
            // Đã bỏ .orderBy để tránh lỗi FAILED_PRECONDITION khi chưa tạo Index
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userId", currentUid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val list = snapshot.mapNotNull { it.toObject(com.example.matcha_vibe.model.Order::class.java) }
                        
                        // Lọc các đơn hàng: Hiện đơn CASH hoặc đơn QR_CODE đã thanh toán (PAID)
                        val filteredList = list.filter { 
                            it.paymentMethod == "CASH" || it.paymentStatus == "PAID"
                        }
                        
                        // Sắp xếp thủ công bằng code thay vì dùng Query (để không cần Index)
                        val sortedList = filteredList.sortedByDescending { it.timestamp }
                        
                        if (sortedList.isEmpty()) {
                            txtNoOrders.visibility = View.VISIBLE
                            rvOrders.visibility = View.GONE
                        } else {
                            txtNoOrders.visibility = View.GONE
                            rvOrders.visibility = View.VISIBLE
                            orderAdapter.updateData(sortedList)
                        }
                    }
                }
        }
    }
}
