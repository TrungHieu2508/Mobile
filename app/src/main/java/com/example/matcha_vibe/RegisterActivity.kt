package com.example.matcha_vibe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.matcha_vibe.model.User

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtName = findViewById<EditText>(R.id.edtName)
        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPhone = findViewById<EditText>(R.id.edtPhone)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val txtGoToLogin = findViewById<TextView>(R.id.txtGoToLogin)

        btnRegister.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val phone = edtPhone.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            // 1. Kiểm tra trống
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.err_empty_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Kiểm tra tên ít nhất 2 từ
            val words = name.split("\\s+".toRegex()).filter { it.isNotEmpty() }
            if (words.size < 2) {
                Toast.makeText(this, getString(R.string.err_name_two_words), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Kiểm tra số điện thoại Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09)
            val phoneRegex = "^(0[35789])[0-9]{8}$".toRegex()
            if (!phoneRegex.matches(phone)) {
                Toast.makeText(this, getString(R.string.err_invalid_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 4. Kiểm tra Email Gmail
            if (!email.lowercase().endsWith("@gmail.com")) {
                Toast.makeText(this, getString(R.string.err_invalid_gmail), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 5. Kiểm tra mật khẩu (Ít nhất 8 ký tự, bao gồm ít nhất 1 chữ và 1 số)
            val hasLetter = password.any { it.isLetter() }
            val hasDigit = password.any { it.isDigit() }

            if (password.length < 8 || !hasLetter || !hasDigit) {
                Toast.makeText(this, getString(R.string.err_password_strength), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = getString(R.string.registering)

            val newUser = User(
                uid = "",
                name = name,
                email = email,
                phone = phone,
                role = "CUSTOMER" // Đăng ký mặc định luôn là CUSTOMER
            )

            FirebaseHelper.register(
                user = newUser,
                password = password,
                onSuccess = {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show()
                    FirebaseHelper.logout()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                },
                onFailure = { e ->
                    btnRegister.isEnabled = true
                    btnRegister.text = getString(R.string.btn_register)
                    Toast.makeText(this, getString(R.string.register_failure, e.message), Toast.LENGTH_LONG).show()
                }
            )
        }

        txtGoToLogin.setOnClickListener {
            finish()
        }
    }
}
