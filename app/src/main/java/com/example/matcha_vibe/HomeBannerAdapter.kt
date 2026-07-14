package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Banner

class HomeBannerAdapter(
    private var banners: List<Banner>
) : RecyclerView.Adapter<HomeBannerAdapter.BannerViewHolder>() {

    fun updateData(newBanners: List<Banner>) {
        banners = newBanners
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_home_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount(): Int = banners.size

    class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgBanner: ImageView = itemView.findViewById(R.id.imgHomeBanner)
        private val txtTitle: TextView = itemView.findViewById(R.id.txtHomeBannerTitle)

        fun bind(banner: Banner) {
            txtTitle.text = banner.content
            FirebaseHelper.loadImage(imgBanner, banner.imageUrl)
        }
    }
}
