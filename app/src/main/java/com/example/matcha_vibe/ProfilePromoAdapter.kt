package com.example.matcha_vibe

import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Promo
import java.text.NumberFormat
import java.util.Locale

class ProfilePromoAdapter(
    private var promos: List<Promo>
) : RecyclerView.Adapter<ProfilePromoAdapter.PromoViewHolder>() {

    fun updateData(newPromos: List<Promo>) {
        promos = newPromos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile_promo, parent, false)
        return PromoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(promos[position])
    }

    override fun getItemCount(): Int = promos.size

    class PromoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPercent: TextView = itemView.findViewById(R.id.txtPromoPercentBadge)
        private val txtCode: TextView = itemView.findViewById(R.id.txtPromoCodeName)
        private val txtMinOrder: TextView = itemView.findViewById(R.id.txtPromoMinOrder)
        private val btnCopy: CardView = itemView.findViewById(R.id.btnCopyPromoCode)

        private val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        fun bind(promo: Promo) {
            txtPercent.text = "${promo.discountPercent}%"
            txtCode.text = promo.code
            txtMinOrder.text = "Đơn từ ${formatter.format(promo.minOrderValue)}"

            btnCopy.setOnClickListener {
                val context = itemView.context
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = android.content.ClipData.newPlainText("MatchaVibePromo", promo.code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Đã sao chép mã ${promo.code} thành công!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
