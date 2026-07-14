package com.example.matcha_vibe.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.matcha_vibe.MainActivity
import com.example.matcha_vibe.OrderActivity
import com.example.matcha_vibe.R
import com.google.zxing.integration.android.IntentIntegrator

class QrScannerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Có thể inflate một layout trống hoặc một hướng dẫn quét
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startQrScan()
    }

    private fun startQrScan() {
        IntentIntegrator.forSupportFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Quét mã QR tại bàn để xem thực đơn và đặt nước")
            .setCameraId(0)
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(true)
            .setOrientationLocked(false)
            .initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(requireContext(), "Đã hủy quét QR", Toast.LENGTH_SHORT).show()
                // Trở về trang chủ nếu hủy
                (activity as? MainActivity)?.let {
                    it.loadFragment(HomeFragment())
                    // Cần cập nhật lại bottom navigation selection nếu cần
                }
            } else {
                handleScannedData(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedData(qrData: String) {
        // Chuyển sang OrderActivity kèm dữ liệu QR và cờ hiệu DINE_IN_FLOW
        val intent = Intent(requireContext(), OrderActivity::class.java).apply {
            putExtra("SCANNED_QR_DATA", qrData)
            putExtra("IS_DINE_IN_FLOW", true)
        }
        startActivity(intent)
        
        // Trở về Home trên BottomNav
        (activity as? MainActivity)?.let {
            it.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)
                ?.selectedItemId = R.id.nav_home
        }
    }
}
