package com.example.matcha_vibe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoContainer = findViewById<LinearLayout>(R.id.logoContainer)
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)

        // Tạo hiệu ứng chuyển động cho logo và text
        val animFadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        animFadeIn.duration = 1500
        logoContainer.startAnimation(animFadeIn)

        // Chạy animation zoom nhẹ logo
        imgLogo.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(1200)
            .withEndAction {
                imgLogo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(800).start()
            }.start()

        // Handler để chuyển trang sau 2.5 giây
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, 2500)
    }

    private fun checkUserSession() {
        try {
            val currentUid = FirebaseHelper.getCurrentUserId()
            if (currentUid == null) {
                // Chưa đăng nhập -> Chuyển đến màn hình Login
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                // Đã đăng nhập -> Lấy thông tin vai trò (role) để chuyển màn hình tương ứng
                FirebaseHelper.getCurrentUser(
                    onSuccess = { user ->
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
                        Toast.makeText(this, "Lỗi kết nối Firebase: ${e.message}", Toast.LENGTH_SHORT).show()
                        // Lỗi -> Fallback mặc định vào màn hình chính khách hàng
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
            // Nếu lỗi nặng, mở màn hình Login để người dùng thử lại
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
