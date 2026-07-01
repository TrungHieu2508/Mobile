package com.example.matcha_vibe

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Khởi tạo Firestore
        val db = Firebase.firestore

        // Tạo một tài liệu test thử nghiệm
        val testData = hashMapOf(
            "name" to "Matcha Latte Đá",
            "price" to 45000,
            "description" to "Matcha nguyên chất kết hợp sữa tươi"
        )

        // Gửi lên collection tên là "test_products"
        db.collection("test_products")
            .add(testData)
            .addOnSuccessListener { documentReference ->
                Log.d("FirebaseTest", "Gửi dữ liệu thành công với ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseTest", "Lỗi khi gửi dữ liệu", e)
            }
    }
}
