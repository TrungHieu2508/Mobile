package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Product

class ProductMiniAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductMiniAdapter.MiniViewHolder>() {

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_mini, parent, false)
        return MiniViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniViewHolder, position: Int) {
        holder.bind(products[position], onItemClick)
    }

    override fun getItemCount(): Int = products.size

    class MiniViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProductMini: ImageView = itemView.findViewById(R.id.imgProductMini)
        private val txtOutOfStockMini: View = itemView.findViewById(R.id.txtOutOfStockMini)
        private val txtProductMiniName: TextView = itemView.findViewById(R.id.txtProductMiniName)

        fun bind(product: Product, click: (Product) -> Unit) {
            txtProductMiniName.text = product.name
            FirebaseHelper.loadImage(imgProductMini, product.imageUrl)
            
            if (product.stockQuantity <= 0) {
                txtOutOfStockMini.visibility = View.VISIBLE
                itemView.alpha = 0.8f
            } else {
                txtOutOfStockMini.visibility = View.GONE
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener { click(product) }
        }
    }
}
