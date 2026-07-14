package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import com.google.android.material.button.MaterialButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OrderAdapter(
    private var orders: List<Order>,
    private val showActions: Boolean = false,
    private val onNextAction: ((Order) -> Unit)? = null,
    private val onCancelAction: ((Order) -> Unit)? = null,
    val showSelection: Boolean = false,
    private val onSelectionChanged: (() -> Unit)? = null,
    private val onDeleteAction: ((Order) -> Unit)? = null
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val localeVN = Locale("vi", "VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val selectedOrderIds = mutableSetOf<String>()

    fun updateData(newOrders: List<Order>) {
        orders = newOrders
        val currentIds = newOrders.map { it.id }.toSet()
        selectedOrderIds.retainAll(currentIds)
        notifyDataSetChanged()
    }

    fun selectAll(select: Boolean) {
        if (select) {
            selectedOrderIds.addAll(orders.map { it.id })
        } else {
            selectedOrderIds.clear()
        }
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position], showActions, onNextAction, onCancelAction, formatter, sdf, showSelection, selectedOrderIds, onSelectionChanged, onDeleteAction)
    }

    override fun getItemCount(): Int = orders.size

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtHeaderType: TextView = itemView.findViewById(R.id.txtOrderHeaderType)
        private val txtStatusBadge: TextView = itemView.findViewById(R.id.txtOrderStatusBadge)
        private val txtTime: TextView = itemView.findViewById(R.id.txtOrderTime)
        private val txtId: TextView = itemView.findViewById(R.id.txtOrderId)
        private val txtDestination: TextView = itemView.findViewById(R.id.txtOrderDestination)
        private val txtItemsSummary: TextView = itemView.findViewById(R.id.txtOrderItemsSummary)
        private val txtPaymentStatus: TextView = itemView.findViewById(R.id.txtOrderPaymentStatus)
        private val txtTotal: TextView = itemView.findViewById(R.id.txtOrderTotal)
        private val chkSelect: CheckBox = itemView.findViewById(R.id.chkOrderSelect)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteOrder)

        private val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutOrderActions)
        private val btnCancel: MaterialButton = itemView.findViewById(R.id.btnOrderActionCancel)
        private val btnNext: MaterialButton = itemView.findViewById(R.id.btnOrderActionNext)

        fun bind(
            order: Order,
            showActions: Boolean,
            onNextAction: ((Order) -> Unit)?,
            onCancelAction: ((Order) -> Unit)?,
            formatter: NumberFormat,
            sdf: SimpleDateFormat,
            showSelection: Boolean,
            selectedIds: MutableSet<String>,
            onSelectionChanged: (() -> Unit)?,
            onDeleteAction: ((Order) -> Unit)?
        ) {
            // Checkbox selection visibility
            if (showSelection) {
                chkSelect.visibility = View.VISIBLE
                chkSelect.setOnCheckedChangeListener(null)
                chkSelect.isChecked = selectedIds.contains(order.id)
                chkSelect.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedIds.add(order.id)
                    } else {
                        selectedIds.remove(order.id)
                    }
                    onSelectionChanged?.invoke()
                }

                // Delete button visibility & callback
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    onDeleteAction?.invoke(order)
                }
            } else {
                chkSelect.visibility = View.GONE
                btnDelete.visibility = View.GONE
            }

            // Hiển thị loại đơn
            if (order.type == "DELIVERY") {
                txtHeaderType.text = "Giao hàng tận nơi"
                txtDestination.text = "Địa chỉ: ${order.address}"
            } else {
                txtHeaderType.text = "Đặt tại bàn ${order.tableNumber}"
                txtDestination.text = "Cơ sở: ${order.storeAddress}"
            }

            // Mã đơn & thời gian
            txtId.text = "Mã: #${order.id.takeLast(6).uppercase()}"
            txtTime.text = sdf.format(Date(order.timestamp))

            // Danh sách món tóm tắt
            val itemsSummary = StringBuilder()
            order.items.forEachIndexed { index, item ->
                itemsSummary.append("${item.quantity}x ${item.productName} (Size: ${item.size}, Đường: ${item.sugar}, Đá: ${item.ice})")
                if (item.note.isNotEmpty()) {
                    itemsSummary.append(" - Ghi chú: ${item.note}")
                }
                if (index < order.items.size - 1) {
                    itemsSummary.append("\n")
                }
            }
            txtItemsSummary.text = itemsSummary.toString()

            // Tổng thanh toán
            txtTotal.text = formatter.format(order.total)

            // Thanh toán
            val methodText = if (order.paymentMethod == "QR_CODE") "QR Code" else "Tiền mặt"
            if (order.paymentStatus == "PAID") {
                txtPaymentStatus.text = "Đã thanh toán ($methodText)"
                txtPaymentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.greenAccent))
            } else {
                txtPaymentStatus.text = "Chưa thanh toán ($methodText)"
                txtPaymentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.redAccent))
            }

            // Trạng thái đơn và Badge màu sắc
            txtStatusBadge.text = getStatusText(order.status)
            updateBadgeStyle(txtStatusBadge, order.status)

            // Làm mờ toàn bộ thẻ nếu đơn bị hủy
            if (order.status == "CANCELLED") {
                itemView.alpha = 0.6f
            } else {
                itemView.alpha = 1.0f
            }

            // Helper function to convert dp to pixels
            fun dpToPx(context: android.content.Context, dp: Int): Int {
                val density = context.resources.displayMetrics.density
                return (dp * density).toInt()
            }

            // Xử lý hiển thị các nút thao tác (Staff/Admin/Customer)
            if (showActions && (order.status != "COMPLETED" && order.status != "CANCELLED")) {
                
                // --- PHÂN QUYỀN HỦY ĐƠN ---
                // onNextAction == null nghĩa là đang ở trang Lịch sử của Khách hàng
                // onNextAction != null nghĩa là đang ở trang Quản lý của Nhân viên/Admin
                val isCustomerPage = onNextAction == null
                
                if (isCustomerPage) {
                    // Khách hàng CHỈ được hủy khi đơn đang ở trạng thái PENDING (Đang chờ)
                    if (order.status == "PENDING") {
                        layoutActions.visibility = View.VISIBLE
                        btnCancel.visibility = View.VISIBLE
                        btnCancel.text = "Hủy đơn hàng"
                    } else {
                        // Nếu đã là PREPARING (Đang pha chế), DELIVERING... thì ẩn nút Hủy của khách
                        layoutActions.visibility = View.GONE
                    }
                } else {
                    // Nhân viên/Admin luôn thấy nút Hủy cho đến khi đơn Hoàn thành
                    layoutActions.visibility = View.VISIBLE
                    btnCancel.visibility = View.VISIBLE
                    btnCancel.text = "Hủy đơn"
                }
                
                // btnCancel is always red Accent with white text, rounded capsule shape
                btnCancel.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.redAccent)
                btnCancel.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                btnCancel.strokeWidth = 0

                // btnNext is only for Staff/Admin (who have onNextAction)
                if (!isCustomerPage) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.white)
                    btnNext.strokeWidth = dpToPx(itemView.context, 1)

                    when (order.status) {
                        "PENDING" -> {
                            btnNext.text = "Duyệt đơn"
                            btnNext.setTextColor(ContextCompat.getColor(itemView.context, R.color.primaryGreen))
                            btnNext.strokeColor = ContextCompat.getColorStateList(itemView.context, R.color.primaryGreen)
                        }
                        "PREPARING" -> {
                            btnNext.text = if (order.type == "DELIVERY") "Giao đi" else "Phục vụ tại bàn"
                            btnNext.setTextColor(ContextCompat.getColor(itemView.context, R.color.primaryBrown))
                            btnNext.strokeColor = ContextCompat.getColorStateList(itemView.context, R.color.primaryBrown)
                        }
                        "DELIVERING" -> {
                            btnNext.text = "Hoàn thành đơn"
                            btnNext.setTextColor(ContextCompat.getColor(itemView.context, R.color.greenAccent))
                            btnNext.strokeColor = ContextCompat.getColorStateList(itemView.context, R.color.greenAccent)
                        }
                        else -> {
                            btnNext.visibility = View.GONE
                        }
                    }
                    btnNext.setOnClickListener { onNextAction.invoke(order) }
                } else {
                    btnNext.visibility = View.GONE
                }

                btnCancel.setOnClickListener { onCancelAction?.invoke(order) }
            } else {
                layoutActions.visibility = View.GONE
            }
        }

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

        private fun updateBadgeStyle(badge: TextView, status: String) {
            val ctx = badge.context
            when (status) {
                "PENDING" -> {
                    badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.accentColor))
                    badge.setTextColor(ContextCompat.getColor(ctx, R.color.primaryBrown))
                }
                "PREPARING" -> {
                    badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.primaryGreenLightest))
                    badge.setTextColor(ContextCompat.getColor(ctx, R.color.primaryGreenDark))
                }
                "DELIVERING" -> {
                    badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.primaryGreenLight))
                    badge.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                "COMPLETED" -> {
                    badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.greenAccent))
                    badge.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
                "CANCELLED" -> {
                    badge.setBackgroundColor(ContextCompat.getColor(ctx, R.color.redAccent))
                    badge.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                }
            }
        }
    }
}
