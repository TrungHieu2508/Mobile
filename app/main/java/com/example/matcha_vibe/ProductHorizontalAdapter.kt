package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Product
import java.text.NumberFormat
import java.util.Locale

class ProductHorizontalAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit,
    private val onAddClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductHorizontalAdapter.HorizontalViewHolder>() {

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product_horizontal, parent, false)
        return HorizontalViewHolder(view)
    }

    override fun onBindViewHolder(holder: HorizontalViewHolder, position: Int) {
        holder.bind(products[position], onItemClick, onAddClick)
    }

    override fun getItemCount(): Int = products.size

    class HorizontalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgProd: ImageView = itemView.findViewById(R.id.imgProdHorizontal)
        private val txtOutOfStockHorizontal: View = itemView.findViewById(R.id.txtOutOfStockHorizontal)
        private val txtName: TextView = itemView.findViewById(R.id.txtProdHorizontalName)
        private val txtDesc: TextView = itemView.findViewById(R.id.txtProdHorizontalDesc)
        private val txtPrice: TextView = itemView.findViewById(R.id.txtProdHorizontalPrice)
        private val btnAdd: ImageView = itemView.findViewById(R.id.btnProdHorizontalAdd)

        fun bind(product: Product, click: (Product) -> Unit, add: (Product) -> Unit) {
            txtName.text = product.name
            txtDesc.text = product.description
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            txtPrice.text = formatter.format(product.price)
            FirebaseHelper.loadImage(imgProd, product.imageUrl)

            if (product.stockQuantity <= 0) {
                btnAdd.visibility = View.GONE
                txtOutOfStockHorizontal.visibility = View.VISIBLE
                itemView.alpha = 0.8f
            } else {
                btnAdd.visibility = View.VISIBLE
                txtOutOfStockHorizontal.visibility = View.GONE
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener { click(product) }
            btnAdd.setOnClickListener { add(product) }
        }
    }
}
