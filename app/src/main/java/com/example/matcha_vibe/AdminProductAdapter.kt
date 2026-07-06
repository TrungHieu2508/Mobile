package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matcha_vibe.model.Product
import java.text.NumberFormat
import java.util.Locale

class AdminProductAdapter(
    private var products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<AdminProductAdapter.AdminProductViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_product, parent, false)
        return AdminProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminProductViewHolder, position: Int) {
        holder.bind(products[position], onEditClick, onDeleteClick, formatter)
    }

    override fun getItemCount(): Int = products.size

    class AdminProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProd: ImageView = itemView.findViewById(R.id.imgAdminProd)
        private val txtOutOfStockOverlay: View = itemView.findViewById(R.id.txtAdminOutOfStockOverlay)
        private val txtName: TextView = itemView.findViewById(R.id.txtAdminProdName)
        private val txtCatPrice: TextView = itemView.findViewById(R.id.txtAdminProdCatPrice)
        private val txtDesc: TextView = itemView.findViewById(R.id.txtAdminProdDesc)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditProd)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteProd)

        fun bind(
            product: Product,
            onEdit: (Product) -> Unit,
            onDelete: (Product) -> Unit,
            formatter: NumberFormat
        ) {
            txtName.text = product.name
            val isOutOfStock = product.stockQuantity <= 0
            val stockHtml = if (isOutOfStock) "<font color='#C94A4A'><b>HẾT HÀNG</b></font>" else "Kho: ${product.stockQuantity}"
            val priceStr = formatter.format(product.price)
            val htmlText = "Danh mục: ${product.category} - Giá: <font color='#C94A4A'><b>$priceStr</b></font><br/>$stockHtml"
            txtCatPrice.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(htmlText)
            }
            txtDesc.text = product.description

            FirebaseHelper.loadImage(imgProd, product.imageUrl)
            txtOutOfStockOverlay.visibility = if (isOutOfStock) View.VISIBLE else View.GONE

            btnEdit.setOnClickListener { onEdit(product) }
            btnDelete.setOnClickListener { onDelete(product) }
        }
    }
}
