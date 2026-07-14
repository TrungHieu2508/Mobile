package com.example.matcha_vibe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtGoToRegister = findViewById<TextView>(R.id.txtGoToRegister)
        val txtForgotPassword = findViewById<TextView>(R.id.txtForgotPassword)

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.err_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = getString(R.string.logging_in)

            FirebaseHelper.login(
                email = email,
                password = password,
                onSuccess = { user ->
                    Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                    when (user.role) {
                        "ADMIN" -> {
                            startActivity(Intent(this, AdminActivity::class.java))
                        }
                        "STAFF" -> {
                            startActivity(Intent(this, StaffActivity::class.java))
                        }
                        else -> {
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                    }
                    finish()
                },
                onFailure = { e ->
                    btnLogin.isEnabled = true
                    btnLogin.text = getString(R.string.btn_login)
                    Toast.makeText(this, getString(R.string.login_failure, e.message), Toast.LENGTH_LONG).show()
                }
            )
        }

        txtForgotPassword.setOnClickListener {
            // Hiển thị thông báo yêu cầu liên hệ Admin qua hotline
            android.app.AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_message)
                .setPositiveButton(R.string.call_now) { _, _ ->
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = android.net.Uri.parse("tel:0948373374")
                    startActivity(intent)
                }
                .setNegativeButton(R.string.close, null)
                .show()
        }

        txtGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
