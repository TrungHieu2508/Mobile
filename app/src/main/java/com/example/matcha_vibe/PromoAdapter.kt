package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Promo
import java.text.NumberFormat
import java.util.Locale

class PromoAdapter(
    private var promos: List<Promo>,
    private val onEditClick: (Promo) -> Unit,
    private val onDeleteClick: (Promo) -> Unit
) : RecyclerView.Adapter<PromoAdapter.PromoViewHolder>() {

    private val localeVN = Locale("vi", "VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)

    fun updateData(newPromos: List<Promo>) {
        promos = newPromos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_promo, parent, false)
        return PromoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(promos[position], onEditClick, onDeleteClick, formatter)
    }

    override fun getItemCount(): Int = promos.size

    class PromoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtCode: TextView = itemView.findViewById(R.id.txtAdminPromoCode)
        private val txtPercent: TextView = itemView.findViewById(R.id.txtAdminPromoPercent)
        private val txtMinOrder: TextView = itemView.findViewById(R.id.txtAdminPromoMinOrder)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditPromo)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeletePromo)

        fun bind(
            promo: Promo,
            onEdit: (Promo) -> Unit,
            onDelete: (Promo) -> Unit,
            formatter: NumberFormat
        ) {
            txtCode.text = promo.code
            txtPercent.text = "Giảm ${promo.discountPercent}%"
            txtMinOrder.text = "Áp dụng đơn từ ${formatter.format(promo.minOrderValue)}"
            btnEdit.setOnClickListener { onEdit(promo) }
            btnDelete.setOnClickListener { onDelete(promo) }
        }
    }
}
