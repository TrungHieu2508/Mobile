package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class OrderCategoryAdapter(
    private var categories: List<String>,
    private var selectedCategory: String,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<OrderCategoryAdapter.CategoryViewHolder>() {

    fun updateSelectedCategory(newSelected: String) {
        selectedCategory = newSelected
        notifyDataSetChanged()
    }

    fun updateData(newCategories: List<String>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position], selectedCategory, onCategoryClick)
    }

    override fun getItemCount(): Int = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btn: MaterialButton = itemView.findViewById(R.id.btnOrderCategoryItem)

        fun bind(category: String, selected: String, click: (String) -> Unit) {
            btn.text = category
            val ctx = itemView.context
            
            if (category.equals(selected, ignoreCase = true)) {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primaryGreen))
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                btn.strokeColor = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primaryGreen))
            } else {
                btn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.white))
                btn.setTextColor(ContextCompat.getColor(ctx, R.color.primaryGreen))
                btn.strokeColor = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.primaryGreen))
            }

            btn.setOnClickListener { click(category) }
        }
    }
}
