package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Store

class StoreAdapter(
    private var stores: List<Store>,
    private val onEditClick: (Store) -> Unit,
    private val onDeleteClick: (Store) -> Unit
) : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

    fun updateData(newStores: List<Store>) {
        stores = newStores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_store, parent, false)
        return StoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
        holder.bind(stores[position], onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = stores.size

    class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtAdminStoreName)
        private val txtAddress: TextView = itemView.findViewById(R.id.txtAdminStoreAddress)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditStore)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteStore)

        fun bind(store: Store, onEdit: (Store) -> Unit, onDelete: (Store) -> Unit) {
            txtName.text = store.name
            txtAddress.text = store.address
            btnEdit.setOnClickListener { onEdit(store) }
            btnDelete.setOnClickListener { onDelete(store) }
        }
    }
}
