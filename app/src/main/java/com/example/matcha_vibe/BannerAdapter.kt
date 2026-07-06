package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Banner

class BannerAdapter(
    private var banners: List<Banner>,
    private val onEditClick: (Banner) -> Unit,
    private val onDeleteClick: (Banner) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    fun updateData(newBanners: List<Banner>) {
        banners = newBanners
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position], onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = banners.size

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgBanner: ImageView = itemView.findViewById(R.id.imgAdminBanner)
        private val txtContent: TextView = itemView.findViewById(R.id.txtAdminBannerContent)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditBanner)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteBanner)

        fun bind(banner: Banner, onEdit: (Banner) -> Unit, onDelete: (Banner) -> Unit) {
            txtContent.text = banner.content
            FirebaseHelper.loadImage(imgBanner, banner.imageUrl)
            btnEdit.setOnClickListener { onEdit(banner) }
            btnDelete.setOnClickListener { onDelete(banner) }
        }
    }
}
