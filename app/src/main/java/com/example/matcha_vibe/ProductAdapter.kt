package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matcha_vibe.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit,
    private val onAddClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position], onItemClick, onAddClick)
    }

    override fun getItemCount(): Int = products.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        private val txtOutOfStockOverlay: View = itemView.findViewById(R.id.txtOutOfStockOverlay)
        private val txtProductName: TextView = itemView.findViewById(R.id.txtProductName)
        private val txtProductDesc: TextView = itemView.findViewById(R.id.txtProductDesc)
        private val txtProductPrice: TextView = itemView.findViewById(R.id.txtProductPrice)
        private val btnAddToCart: ImageView = itemView.findViewById(R.id.btnAddToCart)

        fun bind(product: Product, onItemClick: (Product) -> Unit, onAddClick: (Product) -> Unit) {
            txtProductName.text = product.name
            txtProductDesc.text = product.description

            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            txtProductPrice.text = formatter.format(product.price)

            FirebaseHelper.loadImage(imgProduct, product.imageUrl)

            // Kiểm tra tình trạng kho
            if (product.stockQuantity <= 0) {
                btnAddToCart.visibility = View.GONE
                txtOutOfStockOverlay.visibility = View.VISIBLE
                itemView.alpha = 0.8f
                txtProductPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.redAccent))
            } else {
                btnAddToCart.visibility = View.VISIBLE
                txtOutOfStockOverlay.visibility = View.GONE
                itemView.alpha = 1.0f
                txtProductPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.primaryBrown))
            }

            itemView.setOnClickListener { 
                if (product.stockQuantity > 0) onItemClick(product) 
                else Toast.makeText(itemView.context, "Sản phẩm hiện đang hết hàng!", Toast.LENGTH_SHORT).show()
            }
            btnAddToCart.setOnClickListener { onAddClick(product) }
        }
    }
}
