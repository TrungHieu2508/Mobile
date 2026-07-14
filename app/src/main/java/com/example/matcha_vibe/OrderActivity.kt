package com.example.matcha_vibe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.matcha_vibe.model.Product
import com.example.matcha_vibe.model.Table

class OrderActivity : AppCompatActivity(), CartManager.CartListener {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvProducts: RecyclerView
    private lateinit var edtSearch: EditText
    private lateinit var cardCartBadge: CardView
    private lateinit var txtCartBadgeCount: TextView

    private lateinit var orderCategoryAdapter: OrderCategoryAdapter
    private lateinit var productAdapter: ProductAdapter

    private var allProductsList = listOf<Product>()
    private var filteredList = listOf<Product>()

    private var currentCategory = "Tất cả"
    private var categoriesList = mutableListOf<String>()

    private var isDineInFlow = false
    private var scannedQrData: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        // Bind views
        val btnBack = findViewById<ImageView>(R.id.btnOrderBack)
        val btnCart = findViewById<FrameLayout>(R.id.btnOrderCart)
        cardCartBadge = findViewById(R.id.cardCartBadge)
        txtCartBadgeCount = findViewById(R.id.txtCartBadgeCount)
        edtSearch = findViewById(R.id.edtOrderSearch)
        rvCategories = findViewById(R.id.rvOrderCategories)
        rvProducts = findViewById(R.id.rvOrderProducts)

        // Read intent extra for pre-selected category
        val preSelected = intent.getStringExtra("SELECTED_CATEGORY") ?: "Tất cả"
        currentCategory = preSelected

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Cart button click: Navigate to Cart tab in MainActivity
        btnCart.setOnClickListener {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("SELECT_TAB", "CART")
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(mainIntent)
            finish()
        }

        // Setup Categories Horizontal Recycler
        categoriesList.add("Tất cả")
        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        orderCategoryAdapter = OrderCategoryAdapter(categoriesList, currentCategory) { category ->
            currentCategory = category
            orderCategoryAdapter.updateSelectedCategory(category)
            filterProducts()
        }
        rvCategories.adapter = orderCategoryAdapter

        // Setup Products Grid Recycler (2 columns)
        rvProducts.layoutManager = GridLayoutManager(this, 2)
        productAdapter = ProductAdapter(
            products = emptyList(),
            onItemClick = { product -> showProductDetail(product) },
            onAddClick = { product -> showProductDetail(product) }
        )
        rvProducts.adapter = productAdapter

        // Register Cart Listener
        CartManager.addListener(this)
        updateCartBadge()

        // Load data
        loadCategoriesData()
        loadProductsData()

        // Xử lý dữ liệu QR từ intent nếu có
        scannedQrData = intent.getStringExtra("SCANNED_QR_DATA")
        isDineInFlow = intent.getBooleanExtra("IS_DINE_IN_FLOW", false)
        
        scannedQrData?.let {
            handleScannedQrCode(it)
        }

        // Search text watcher
        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadCategoriesData() {
        FirebaseHelper.getCategories(
            onSuccess = { list ->
                categoriesList.clear()
                categoriesList.add("Tất cả")
                categoriesList.addAll(list.map { it.name })
                orderCategoryAdapter.updateData(categoriesList)
                
                // Cập nhật vị trí danh mục được chọn
                val index = categoriesList.indexOfFirst { it.equals(currentCategory, ignoreCase = true) }
                if (index >= 0) {
                    rvCategories.scrollToPosition(index)
                }
            },
            onFailure = {
                Toast.makeText(this, "Lỗi tải danh mục: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadProductsData() {
        FirebaseHelper.getProducts(
            onSuccess = { list ->
                allProductsList = list.filter { it.available }
                filterProducts()
            },
            onFailure = {
                Toast.makeText(this, "Lỗi tải món nước: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun filterProducts() {
        val query = edtSearch.text.toString().trim()
        
        // 1. Lọc theo danh mục
        var list = if (currentCategory.equals("Tất cả", ignoreCase = true)) {
            allProductsList
        } else {
            allProductsList.filter { it.category.equals(currentCategory, ignoreCase = true) }
        }

        // 2. Lọc theo ô tìm kiếm
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }

        filteredList = list
        productAdapter.updateData(filteredList)
    }

    private fun showProductDetail(product: Product) {
        val bottomSheet = ProductDetailBottomSheet(product) {
            // Đã thêm thành công
            if (isDineInFlow) {
                // Nếu là luồng đặt tại bàn từ QR, chuyển thẳng tới giỏ hàng
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    putExtra("SELECT_TAB", "CART")
                    putExtra("ORDER_TYPE", "DINE_IN")
                    putExtra("SCANNED_QR_DATA", scannedQrData)
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                startActivity(mainIntent)
                finish()
            }
        }
        bottomSheet.show(supportFragmentManager, "ProductDetailBottomSheet")
    }

    private fun handleScannedQrCode(qrData: String) {
        try {
            // Logic xử lý tương tự CartFragment
            val regex = Regex("(.+) [-] (bàn|Bàn|ban|Ban) (\\d+)", RegexOption.IGNORE_CASE)
            val matchResult = regex.find(qrData)

            if (matchResult != null) {
                val storeName = matchResult.groupValues[1].trim()
                val tableNumber = matchResult.groupValues[3].trim()
                showScannedTableDialog(storeName, tableNumber)
            } else if (qrData.contains(" - ", ignoreCase = true)) {
                val parts = qrData.split(" - ")
                if (parts.size >= 2) {
                    val storeName = parts[0].trim()
                    val tableNumber = parts[1].replace(Regex("[^0-9]"), "")
                    showScannedTableDialog(storeName, tableNumber)
                }
            } else if (qrData.startsWith("matchavibe://")) {
                val uri = Uri.parse(qrData)
                val storeId = uri.getQueryParameter("storeId")
                val tableNumber = uri.getQueryParameter("tableNumber")
                if (storeId != null && tableNumber != null) {
                    // Thử lấy thông tin chi tiết từ Firebase
                    FirebaseHelper.getTables(
                        onSuccess = { list ->
                            val table = list.find { it.storeId == storeId && it.tableNumber == tableNumber }
                            if (table != null) {
                                showScannedTableDialog(table.storeAddress, table.tableNumber)
                            } else {
                                showScannedTableDialog("Cửa hàng ID: $storeId", tableNumber)
                            }
                        },
                        onFailure = {
                            showScannedTableDialog("Cửa hàng ID: $storeId", tableNumber)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi giải mã QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showScannedTableDialog(storeName: String, tableNumber: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Chào mừng bạn!")
            .setMessage("Bạn đang ở $storeName - Bàn số $tableNumber.\nMời bạn chọn món nước yêu thích!")
            .setPositiveButton("Bắt đầu chọn món") { d, _ -> d.dismiss() }
            .create()
        dialog.show()
    }

    private fun updateCartBadge() {
        val count = CartManager.getCartCount()
        if (count > 0) {
            cardCartBadge.visibility = View.VISIBLE
            txtCartBadgeCount.text = count.toString()
        } else {
            cardCartBadge.visibility = View.GONE
        }
    }

    // Callback từ CartListener
    override fun onCartChanged() {
        runOnUiThread {
            updateCartBadge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister listener to prevent leak
        CartManager.removeListener(this)
    }
}
