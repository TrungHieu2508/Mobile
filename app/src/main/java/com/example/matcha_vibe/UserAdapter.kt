package com.example.matcha_vibe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.User

class UserAdapter(
    private var users: List<User>,
    private val onChangeRole: (User, String) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], onChangeRole, onDeleteClick)
    }

    override fun getItemCount(): Int = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtName: TextView = itemView.findViewById(R.id.txtAdminUserName)
        private val txtRole: TextView = itemView.findViewById(R.id.txtAdminUserRole)
        private val txtEmail: TextView = itemView.findViewById(R.id.txtAdminUserEmail)
        private val txtPhone: TextView = itemView.findViewById(R.id.txtAdminUserPhone)
        private val btnCustomer: Button = itemView.findViewById(R.id.btnMakeCustomer)
        private val btnStaff: Button = itemView.findViewById(R.id.btnMakeStaff)
        private val btnAdmin: Button = itemView.findViewById(R.id.btnMakeAdmin)
        private val btnDelete: View = itemView.findViewById(R.id.btnDeleteUser)

        fun bind(user: User, onChangeRole: (User, String) -> Unit, onDeleteClick: (User) -> Unit) {
            txtName.text = user.name
            txtRole.text = user.role
            txtEmail.text = "Email: ${user.email}"
            txtPhone.text = "SĐT: ${if (user.phone.isNotEmpty()) user.phone else "Chưa có"}"

            btnCustomer.setOnClickListener { onChangeRole(user, "CUSTOMER") }
            btnStaff.setOnClickListener { onChangeRole(user, "STAFF") }
            btnAdmin.setOnClickListener { onChangeRole(user, "ADMIN") }
            btnDelete.setOnClickListener { onDeleteClick(user) }
        }
    }
}
