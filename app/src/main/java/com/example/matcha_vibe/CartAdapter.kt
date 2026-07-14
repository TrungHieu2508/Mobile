package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.CartItem
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val cartItems: List<CartItem>,
    private val onRemoveItem: (Int) -> Unit,
    private val onQtyChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val localeVN = Locale("vi", "VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position], position, onRemoveItem, onQtyChanged, formatter)
    }

    override fun getItemCount(): Int = cartItems.size

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProd: ImageView = itemView.findViewById(R.id.imgCartItem)
        private val txtName: TextView = itemView.findViewById(R.id.txtCartItemName)
        private val txtOptions: TextView = itemView.findViewById(R.id.txtCartItemOptions)
        private val txtNote: TextView = itemView.findViewById(R.id.txtCartItemNote)
        private val txtPriceQty: TextView = itemView.findViewById(R.id.txtCartItemPrice)
        private val txtTotalPrice: TextView = itemView.findViewById(R.id.txtCartItemTotalPrice)
        private val btnRemove: ImageView = itemView.findViewById(R.id.btnRemoveCartItem)
        private val btnQtyMinus: TextView = itemView.findViewById(R.id.btnCartQtyMinus)
        private val txtQtyCount: TextView = itemView.findViewById(R.id.txtCartQtyCount)
        private val btnQtyPlus: TextView = itemView.findViewById(R.id.btnCartQtyPlus)

        fun bind(
            item: CartItem,
            position: Int,
            onRemoveItem: (Int) -> Unit,
            onQtyChanged: (Int, Int) -> Unit,
            formatter: NumberFormat
        ) {
            txtName.text = item.productName
            // Hiển thị các tùy chọn (chỉ hiển thị nếu có giá trị)
            val optionsList = mutableListOf<String>()
            if (item.size.isNotEmpty()) optionsList.add("Size: ${item.size}")
            if (item.sugar.isNotEmpty()) optionsList.add("Đường: ${item.sugar}")
            if (item.ice.isNotEmpty()) optionsList.add("Đá: ${item.ice}")
            
            if (optionsList.isNotEmpty()) {
                txtOptions.text = optionsList.joinToString(" | ")
                txtOptions.visibility = View.VISIBLE
            } else {
                txtOptions.visibility = View.GONE
            }
            
            FirebaseHelper.loadImage(imgProd, item.productImageUrl)
            txtQtyCount.text = item.quantity.toString()

            if (item.note.isNotEmpty()) {
                txtNote.text = "Ghi chú: ${item.note}"
                txtNote.visibility = View.VISIBLE
            } else {
                txtNote.visibility = View.GONE
            }

            txtPriceQty.text = "${formatter.format(item.price)} x ${item.quantity}"
            txtTotalPrice.text = formatter.format(item.getTotalPrice())

            btnRemove.setOnClickListener {
                onRemoveItem(position)
            }

            btnQtyMinus.setOnClickListener {
                if (item.quantity > 1) {
                    onQtyChanged(position, item.quantity - 1)
                } else {
                    onRemoveItem(position)
                }
            }

            btnQtyPlus.setOnClickListener {
                onQtyChanged(position, item.quantity + 1)
            }
        }
    }
}
