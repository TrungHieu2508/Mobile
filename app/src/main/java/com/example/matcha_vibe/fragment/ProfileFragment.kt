package com.example.matcha_vibe.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.*
import com.example.matcha_vibe.model.User

class ProfileFragment : Fragment() {

    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtName = view.findViewById<TextView>(R.id.txtProfileName)
        val txtRole = view.findViewById<TextView>(R.id.txtProfileRoleBadge)
        val txtEmail = view.findViewById<TextView>(R.id.txtProfileEmail)
        
        val edtProfileName = view.findViewById<EditText>(R.id.edtProfileName)
        val edtProfilePhone = view.findViewById<EditText>(R.id.edtProfilePhone)
        val edtProfileEmail = view.findViewById<EditText>(R.id.edtProfileEmail)

        val btnConfirmUpdate = view.findViewById<Button>(R.id.btnConfirmUpdate)
        val btnChangePassword = view.findViewById<Button>(R.id.btnChangePassword)

        val btnAdmin = view.findViewById<Button>(R.id.btnOpenAdminPanel)
        val btnStaff = view.findViewById<Button>(R.id.btnOpenStaffPanel)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)
        
        // Tải thông tin người dùng từ Firestore
        fun loadUserData() {
            FirebaseHelper.getCurrentUser(
                onSuccess = { user ->
                    currentUser = user
                    txtName.text = user.name
                    txtEmail.text = user.email

                    // Set text to inline input fields
                    edtProfileName.setText(user.name)
                    edtProfilePhone.setText(user.phone)
                    edtProfileEmail.setText(user.email)

                    // Reset shortcuts visibility
                    btnAdmin.visibility = View.GONE
                    btnStaff.visibility = View.GONE

                    when (user.role) {
                        "ADMIN" -> {
                            txtRole.text = "Quản trị viên (ADMIN)"
                            btnAdmin.visibility = View.VISIBLE
                            btnStaff.visibility = View.VISIBLE // Admin mở được cả hai
                        }
                        "STAFF" -> {
                            txtRole.text = "Nhân viên (STAFF)"
                            btnStaff.visibility = View.VISIBLE
                        }
                        else -> {
                            txtRole.text = "Khách hàng (CUSTOMER)"
                            
                            // Load promos for Customer
                            loadCustomerPromos(view)
                        }
                    }
                },
                onFailure = { e ->
                    Toast.makeText(requireContext(), "Lỗi tải thông tin cá nhân: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Tải dữ liệu ban đầu
        loadUserData()

        // Button Cập nhật thông tin cá nhân directly inline
        btnConfirmUpdate.setOnClickListener {
            currentUser?.let { user ->
                val newName = edtProfileName.text.toString().trim()
                val newPhone = edtProfilePhone.text.toString().trim()

                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), "Họ tên không được để trống", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnConfirmUpdate.isEnabled = false
                btnConfirmUpdate.text = "Đang cập nhật..."

                FirebaseHelper.updateUserProfile(user.uid, newName, newPhone,
                    onSuccess = {
                        btnConfirmUpdate.isEnabled = true
                        btnConfirmUpdate.text = "Xác nhận cập nhật"
                        Toast.makeText(requireContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show()
                        loadUserData() // reload profile data on success
                    },
                    onFailure = { e ->
                        btnConfirmUpdate.isEnabled = true
                        btnConfirmUpdate.text = "Xác nhận cập nhật"
                        Toast.makeText(requireContext(), "Cập nhật thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Button Đổi mật khẩu
        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Chuyển trang quản lý
        btnAdmin.setOnClickListener {
            startActivity(Intent(requireContext(), AdminActivity::class.java))
        }

        btnStaff.setOnClickListener {
            startActivity(Intent(requireContext(), StaffActivity::class.java))
        }

        // Đăng xuất
        btnLogout.setOnClickListener {
            FirebaseHelper.logout()
            CartManager.clearCart() // Xóa giỏ hàng khi đăng xuất
            Toast.makeText(requireContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            activity?.finishAffinity()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val edtCurrent = dialogView.findViewById<EditText>(R.id.edtDialogCurrentPassword)
        val edtNew = dialogView.findViewById<EditText>(R.id.edtDialogNewPassword)
        val edtConfirm = dialogView.findViewById<EditText>(R.id.edtDialogConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Đổi mật khẩu") { dialog, _ ->
                val currentPw = edtCurrent.text.toString().trim()
                val newPw = edtNew.text.toString().trim()
                val confirmPw = edtConfirm.text.toString().trim()

                if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPw.length < 6) {
                    Toast.makeText(requireContext(), "Mật khẩu mới phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPw != confirmPw) {
                    Toast.makeText(requireContext(), "Xác nhận mật khẩu mới không trùng khớp", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                FirebaseHelper.changePassword(currentPw, newPw,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Lỗi đổi mật khẩu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadCustomerPromos(view: View) {
        val layoutPromos = view.findViewById<View>(R.id.layoutProfilePromos)
        val rv = view.findViewById<RecyclerView>(R.id.rvCustomerPromos)

        layoutPromos.visibility = View.VISIBLE
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        
        val promoAdapter = ProfilePromoAdapter(emptyList())
        rv.adapter = promoAdapter

        FirebaseHelper.getPromos(
            onSuccess = { list ->
                promoAdapter.updateData(list)
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Lỗi tải danh sách khuyến mãi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
