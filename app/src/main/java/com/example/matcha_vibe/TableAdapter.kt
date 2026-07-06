package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Table

class TableAdapter(
    private var tables: List<Table>,
    private val onViewQr: (Table) -> Unit,
    private val onEditClick: (Table) -> Unit,
    private val onDeleteClick: (Table) -> Unit
) : RecyclerView.Adapter<TableAdapter.TableViewHolder>() {

    fun updateData(newTables: List<Table>) {
        tables = newTables
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_table, parent, false)
        return TableViewHolder(view)
    }

    override fun onBindViewHolder(holder: TableViewHolder, position: Int) {
        holder.bind(tables[position], onViewQr, onEditClick, onDeleteClick)
    }

    override fun getItemCount(): Int = tables.size

    class TableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtNum: TextView = itemView.findViewById(R.id.txtAdminTableNum)
        private val txtStore: TextView = itemView.findViewById(R.id.txtAdminTableStoreAddress)
        private val btnQr: ImageView = itemView.findViewById(R.id.btnViewQrCode)
        private val btnEdit: ImageView = itemView.findViewById(R.id.btnEditTable)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDeleteTable)

        fun bind(
            table: Table,
            onViewQr: (Table) -> Unit,
            onEdit: (Table) -> Unit,
            onDelete: (Table) -> Unit
        ) {
            txtNum.text = "Bàn số ${table.tableNumber}"
            txtStore.text = "Cơ sở: ${table.storeAddress}"
            btnQr.setOnClickListener { onViewQr(table) }
            btnEdit.setOnClickListener { onEdit(table) }
            btnDelete.setOnClickListener { onDelete(table) }
        }
    }
}
