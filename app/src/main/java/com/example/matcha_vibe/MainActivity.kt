package com.example.matcha_vibe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.matcha_vibe.fragment.CartFragment
import com.example.matcha_vibe.fragment.HomeFragment
import com.example.matcha_vibe.fragment.OrdersFragment
import com.example.matcha_vibe.fragment.ProfileFragment
import com.example.matcha_vibe.fragment.QrScannerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), CartManager.CartListener {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottomNavigation)

        // Load HomeFragment mặc định hoặc chuyển hướng tab nếu có extra
        handleIntent(intent)

        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_qr -> QrScannerFragment()
                R.id.nav_cart -> CartFragment()
                R.id.nav_orders -> OrdersFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }

        // Đăng ký lắng nghe thay đổi giỏ hàng
        CartManager.addListener(this)
        updateCartBadge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val tab = intent.getStringExtra("SELECT_TAB")
        if (tab == "CART") {
            val orderType = intent.getStringExtra("ORDER_TYPE")
            val qrData = intent.getStringExtra("SCANNED_QR_DATA")
            
            val cartFragment = CartFragment().apply {
                arguments = Bundle().apply {
                    putString("ORDER_TYPE", orderType)
                    putString("SCANNED_QR_DATA", qrData)
                }
            }
            
            // Cập nhật BottomNav mà không trigger listener để tránh load fragment 2 lần
            bottomNavigation.setOnItemSelectedListener(null)
            bottomNavigation.selectedItemId = R.id.nav_cart
            
            // Re-bind listener
            bottomNavigation.setOnItemSelectedListener { item ->
                val fragment: Fragment = when (item.itemId) {
                    R.id.nav_home -> HomeFragment()
                    R.id.nav_qr -> QrScannerFragment()
                    R.id.nav_cart -> CartFragment()
                    R.id.nav_orders -> OrdersFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> HomeFragment()
                }
                loadFragment(fragment)
                true
            }

            loadFragment(cartFragment)
        } else {
            if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) == null) {
                loadFragment(HomeFragment())
            }
        }
    }

    private fun updateCartBadge() {
        if (!::bottomNavigation.isInitialized) return
        val count = CartManager.getCartCount()
        val badge = bottomNavigation.getOrCreateBadge(R.id.nav_cart)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.backgroundColor = ContextCompat.getColor(this, R.color.redAccent)
            badge.badgeTextColor = ContextCompat.getColor(this, R.color.white)
        } else {
            badge.isVisible = false
        }
    }

    override fun onCartChanged() {
        runOnUiThread {
            updateCartBadge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký lắng nghe giỏ hàng tránh memory leak
        CartManager.removeListener(this)
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
