package com.example.matcha_vibe

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matcha_vibe.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {

    // Sub-layouts
    private lateinit var layoutDashboard: View
    private lateinit var layoutProducts: View
    private lateinit var layoutStores: View
    private lateinit var layoutTables: View
    private lateinit var layoutPromos: View
    private lateinit var layoutUsers: View
    private lateinit var layoutOrders: View
    private lateinit var layoutAdminCategories: View
    private lateinit var layoutAdminBanners: View

    private lateinit var txtAdminTitle: TextView

    // Adapters
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var promoAdapter: PromoAdapter
    private lateinit var userAdapter: UserAdapter
    private lateinit var adminProductAdapter: AdminProductAdapter
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var tableAdapter: TableAdapter

    // Editing States
    private var editingProduct: Product? = null
    private var editingStore: Store? = null
    private var editingPromo: Promo? = null
    private var editingCategory: Category? = null
    private var editingBanner: Banner? = null
    private var editingTable: Table? = null

    // User Filter Role State
    private var selectedRoleFilter = "CUSTOMER" // Mặc định hiển thị khách hàng đầu tiên
    private var allUsersList = listOf<User>()
    private var dynamicCategoriesList = listOf<Category>()
    private var allOrdersList = listOf<Order>()
    private var selectedChartMode = "DAY" // "DAY", "MONTH", "YEAR"

    private lateinit var btnChartDay: TextView
    private lateinit var btnChartMonth: TextView
    private lateinit var btnChartYear: TextView
    private lateinit var layoutAdminChartBars: LinearLayout

    private lateinit var pickImageLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var selectedProductImageBase64: String = ""
    private var selectedBannerImageBase64: String = ""
    private var imagePickerTarget = "PRODUCT" // "PRODUCT" or "BANNER"

    private val localeVN = Locale.forLanguageTag("vi-VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                try {
                    val base64 = convertImageUriToBase64(uri)
                    if (base64 != null) {
                        if (imagePickerTarget == "PRODUCT") {
                            selectedProductImageBase64 = base64
                            findViewById<TextView>(R.id.txtImageStatus).text = "Đã chọn ảnh thành công"
                            FirebaseHelper.loadImage(findViewById(R.id.imgProductPreview), selectedProductImageBase64)
                        } else if (imagePickerTarget == "BANNER") {
                            selectedBannerImageBase64 = base64
                            findViewById<TextView>(R.id.txtBannerImageStatus).text = "Đã chọn ảnh thành công"
                            FirebaseHelper.loadImage(findViewById(R.id.imgBannerPreview), selectedBannerImageBase64)
                        }
                    } else {
                        Toast.makeText(this, "Lỗi nén ảnh, vui lòng thử ảnh khác", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Lỗi đọc ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Bind Sub-layouts
        layoutDashboard = findViewById(R.id.layoutAdminDashboard)
        layoutProducts = findViewById(R.id.layoutAdminProducts)
        layoutStores = findViewById(R.id.layoutAdminStores)
        layoutTables = findViewById(R.id.layoutAdminTables)
        layoutPromos = findViewById(R.id.layoutAdminPromos)
        layoutUsers = findViewById(R.id.layoutAdminUsers)
        layoutOrders = findViewById(R.id.layoutAdminOrders)
        layoutAdminCategories = findViewById(R.id.layoutAdminCategories)
        layoutAdminBanners = findViewById(R.id.layoutAdminBanners)

        txtAdminTitle = findViewById(R.id.txtAdminTitle)

        val btnBackToClient = findViewById<View>(R.id.btnAdminBackToClient)

        // --- Navigation Switcher ---
        findViewById<View>(R.id.btnMenuProducts).setOnClickListener { showSubPanel("PRODUCTS") }
        findViewById<View>(R.id.btnMenuStores).setOnClickListener { showSubPanel("STORES") }
        findViewById<View>(R.id.btnMenuPromos).setOnClickListener { showSubPanel("PROMOS") }
        findViewById<View>(R.id.btnMenuUsers).setOnClickListener { showSubPanel("USERS") }
        findViewById<View>(R.id.btnMenuOrdersAdmin).setOnClickListener { showSubPanel("ORDERS") }
        findViewById<View>(R.id.btnMenuCategories).setOnClickListener { showSubPanel("CATEGORIES") }
        findViewById<View>(R.id.btnMenuBanners).setOnClickListener { showSubPanel("BANNERS") }

        btnBackToClient.setOnClickListener {
            if (layoutDashboard.visibility != View.VISIBLE) {
                showSubPanel("DASHBOARD")
            } else {
                FirebaseHelper.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        // Tải thông số thống kê tổng quát
        loadDashboardMetrics()

        // Khởi tạo các modul con
        initStoresModule()
        initProductsModule()
        initPromosModule()
        initUsersModule()
        initOrdersMonitorModule()
        initCategoriesModule()
        initBannersModule()
        initTablesModule()
        setupChartModule()

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (layoutDashboard.visibility != View.VISIBLE) {
                    showSubPanel("DASHBOARD")
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // Remove the deprecated onBackPressed

    private fun showSubPanel(panelName: String) {
        layoutDashboard.visibility = View.GONE
        layoutProducts.visibility = View.GONE
        layoutStores.visibility = View.GONE
        layoutTables.visibility = View.GONE
        layoutPromos.visibility = View.GONE
        layoutUsers.visibility = View.GONE
        layoutOrders.visibility = View.GONE
        layoutAdminCategories.visibility = View.GONE
        layoutAdminBanners.visibility = View.GONE

        // Reset editing states when switching panel
        resetEditingProductState()
        resetEditingStoreState()
        resetEditingPromoState()
        resetEditingCategoryState()
        resetEditingBannerState()

        when (panelName) {
            "DASHBOARD" -> {
                layoutDashboard.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN TRỊ VIÊN (ADMIN)"
                loadDashboardMetrics()
            }
            "PRODUCTS" -> {
                layoutProducts.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ SẢN PHẨM"
                loadCategoriesForProductSpinner() // Load dynamic categories spinner
                loadProductsAdmin()
            }
            "STORES" -> {
                layoutStores.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ CƠ SỞ"
                loadStoresAdmin()
            }
            "PROMOS" -> {
                layoutPromos.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ KHUYẾN MÃI"
                loadPromosAdmin()
            }
            "USERS" -> {
                layoutUsers.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ NHÂN SỰ"
                loadUsersAdmin()
            }
            "ORDERS" -> {
                layoutOrders.visibility = View.VISIBLE
                txtAdminTitle.text = "GIÁM SÁT ĐƠN HÀNG"
                if (::orderAdapter.isInitialized) {
                    orderAdapter.selectedOrderIds.clear()
                    updateOrdersActionBarUI()
                }
                loadOrdersAdmin()
            }
            "CATEGORIES" -> {
                layoutAdminCategories.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ DANH MỤC"
                loadCategoriesAdmin()
            }
            "BANNERS" -> {
                layoutAdminBanners.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ BANNER"
                loadBannersAdmin()
            }
            "TABLES" -> {
                layoutTables.visibility = View.VISIBLE
                txtAdminTitle.text = "QUẢN LÝ BÀN"
                loadStoresForTableSpinner()
                loadTablesAdmin()
            }
        }
    }

    private fun loadDashboardMetrics() {
        val txtRevenue = findViewById<TextView>(R.id.txtMetricRevenue)
        val txtOrdersCount = findViewById<TextView>(R.id.txtMetricOrdersCount)

        FirebaseHelper.getAllOrdersRealtime(
            onUpdate = { orders ->
                // Lọc đơn hàng hợp lệ: Tiền mặt hoặc đã thanh toán thành công, VÀ không bị hủy
                val validOrders = orders.filter { 
                    (it.paymentMethod == "CASH" || it.paymentStatus == "PAID") && it.status != "CANCELLED"
                }
                
                allOrdersList = validOrders
                txtOrdersCount.text = "${validOrders.size} đơn"
                val completedOrders = validOrders.filter { it.status == "COMPLETED" }
                val totalRev = completedOrders.sumOf { it.total }
                txtRevenue.text = formatter.format(totalRev)
                updateRevenueChart()
            },
            onFailure = {}
        )
    }

    // --- 1. Quản lý Cơ sở (CRUD) ---
    private fun initStoresModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminStores)
        val nameInput = findViewById<EditText>(R.id.edtStoreName)
        val addrInput = findViewById<EditText>(R.id.edtStoreAddress)
        val btnGetCoords = findViewById<Button>(R.id.btnGetCoordsFromAddr)
        val latInput = findViewById<EditText>(R.id.edtStoreLat)
        val lngInput = findViewById<EditText>(R.id.edtStoreLng)
        val saveBtn = findViewById<Button>(R.id.btnAddStore)

        btnGetCoords.setOnClickListener {
            val address = addrInput.text.toString().trim()
            if (address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập địa chỉ trước", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Hiển thị trạng thái đang tìm
            btnGetCoords.text = "Đang tìm..."
            btnGetCoords.isEnabled = false

            try {
                val geocoder = android.location.Geocoder(this, Locale.getDefault())
                val addressListener = android.location.Geocoder.GeocodeListener { addresses ->
                    runOnUiThread {
                        btnGetCoords.text = "Tìm tọa độ từ địa chỉ trên"
                        btnGetCoords.isEnabled = true
                        if (addresses.isNotEmpty()) {
                            val location = addresses[0]
                            findViewById<EditText>(R.id.edtStoreLat).setText("%.6f".format(Locale.US, location.latitude))
                            findViewById<EditText>(R.id.edtStoreLng).setText("%.6f".format(Locale.US, location.longitude))
                            Toast.makeText(this, "Đã khớp: ${location.getAddressLine(0)}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Không tìm thấy tọa độ.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocationName(address, 3, addressListener)
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(address, 3)
                    runOnUiThread {
                        btnGetCoords.text = "Tìm tọa độ từ địa chỉ trên"
                        btnGetCoords.isEnabled = true
                        if (!addresses.isNullOrEmpty()) {
                            val location = addresses[0]
                            findViewById<EditText>(R.id.edtStoreLat).setText("%.6f".format(Locale.US, location.latitude))
                            findViewById<EditText>(R.id.edtStoreLng).setText("%.6f".format(Locale.US, location.longitude))
                            Toast.makeText(this, "Đã khớp: ${location.getAddressLine(0)}", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "Không tìm thấy tọa độ.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                btnGetCoords.text = "Tìm tọa độ từ địa chỉ trên"
                btnGetCoords.isEnabled = true
                Toast.makeText(this, "Lỗi tìm tọa độ: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        rv.layoutManager = LinearLayoutManager(this)
        storeAdapter = StoreAdapter(
            stores = emptyList(),
            onEditClick = { store ->
                editingStore = store
                nameInput.setText(store.name)
                addrInput.setText(store.address)
                latInput.setText(store.lat.toString())
                lngInput.setText(store.lng.toString())
                saveBtn.text = "CẬP NHẬT CƠ SỞ"
                saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBrown))
                findViewById<TextView>(R.id.txtStoreFormTitle).text = "Chỉnh sửa cơ sở cửa hàng"
            },
            onDeleteClick = { store ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa cơ sở")
                    .setMessage("Bạn có chắc chắn muốn xóa cơ sở ${store.name} không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deleteStore(store.id,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa cơ sở thành công!", Toast.LENGTH_SHORT).show()
                                loadStoresAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = storeAdapter

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val address = addrInput.text.toString().trim()
            // Xử lý dấu phẩy thành dấu chấm để tránh lỗi toDoubleOrNull
            val latStr = latInput.text.toString().trim().replace(",", ".")
            val lngStr = lngInput.text.toString().trim().replace(",", ".")

            if (name.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ tên và địa chỉ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lat = latStr.toDoubleOrNull() ?: 0.0
            val lng = lngStr.toDoubleOrNull() ?: 0.0

            val storeToSave = editingStore?.copy(name = name, address = address, lat = lat, lng = lng) 
                ?: Store("", name, address, lat, lng)

            if (editingStore != null) {
                FirebaseHelper.updateStore(storeToSave,
                    onSuccess = {
                        Toast.makeText(this, "Cập nhật cơ sở thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingStoreState()
                        loadStoresAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addStore(storeToSave,
                    onSuccess = {
                        Toast.makeText(this, "Thêm cơ sở thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingStoreState()
                        loadStoresAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun resetEditingStoreState() {
        editingStore = null
        findViewById<EditText>(R.id.edtStoreName).text.clear()
        findViewById<EditText>(R.id.edtStoreAddress).text.clear()
        findViewById<EditText>(R.id.edtStoreLat).text.clear()
        findViewById<EditText>(R.id.edtStoreLng).text.clear()
        val saveBtn = findViewById<Button>(R.id.btnAddStore)
        saveBtn.text = "LƯU CƠ SỞ"
        saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryGreen))
        findViewById<TextView>(R.id.txtStoreFormTitle).text = "Thêm cơ sở cửa hàng"
    }

    private fun loadStoresAdmin() {
        FirebaseHelper.getStores(
            onSuccess = { stores ->
                runOnUiThread {
                    storeAdapter.updateData(stores)
                    // Thông báo chi tiết tên các cơ sở để kiểm tra
                    if (layoutStores.visibility == View.VISIBLE) {
                        val storeNames = stores.joinToString(", ") { s -> s.name }
                        Toast.makeText(this, "Hệ thống đã nạp ${stores.size} cơ sở: $storeNames", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onFailure = {
                Toast.makeText(this, "Lỗi tải: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }



    // --- 3. Quản lý Sản phẩm (CRUD) ---
    private fun loadCategoriesForProductSpinner() {
        val spnCat = findViewById<Spinner>(R.id.spnProdCategory)
        FirebaseHelper.getCategories(
            onSuccess = { list ->
                dynamicCategoriesList = list
                val names = list.map { it.name }
                val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnCat.adapter = spinnerAdapter
                
                // Chọn lại danh mục cũ nếu đang edit
                editingProduct?.let { prod ->
                    val pos = names.indexOf(prod.category)
                    if (pos >= 0) {
                        spnCat.setSelection(pos)
                    }
                }
            },
            onFailure = {
                Toast.makeText(this, "Không thể tải danh mục: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun initProductsModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminProducts)
        val edtName = findViewById<EditText>(R.id.edtProdName)
        val spnCat = findViewById<Spinner>(R.id.spnProdCategory)
        val edtPrice = findViewById<EditText>(R.id.edtProdPrice)
        val edtStock = findViewById<EditText>(R.id.edtProdStock)
        val edtDesc = findViewById<EditText>(R.id.edtProdDesc)
        val btnSelectImg = findViewById<Button>(R.id.btnSelectProductImage)
        val saveBtn = findViewById<Button>(R.id.btnAddProduct)

        btnSelectImg.setOnClickListener {
            imagePickerTarget = "PRODUCT"
            pickImageLauncher.launch("image/*")
        }

        rv.layoutManager = LinearLayoutManager(this)
        adminProductAdapter = AdminProductAdapter(
            products = emptyList(),
            onEditClick = { product ->
                editingProduct = product
                edtName.setText(product.name)
                edtPrice.setText(product.price.toInt().toString())
                edtStock.setText(product.stockQuantity.toString())
                edtDesc.setText(product.description)
                
                selectedProductImageBase64 = product.imageUrl
                findViewById<TextView>(R.id.txtImageStatus).text = if (product.imageUrl.isNotEmpty()) "Ảnh hiện tại" else "Chưa có ảnh"
                FirebaseHelper.loadImage(findViewById(R.id.imgProductPreview), selectedProductImageBase64)
                
                val names = dynamicCategoriesList.map { it.name }
                val catPos = names.indexOf(product.category)
                if (catPos >= 0) {
                    spnCat.setSelection(catPos)
                }

                saveBtn.text = "CẬP NHẬT SẢN PHẨM"
                saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBrown))
                findViewById<TextView>(R.id.txtProductFormTitle).text = "Chỉnh sửa sản phẩm"
            },
            onDeleteClick = { product ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa sản phẩm")
                    .setMessage("Bạn có muốn xóa món ${product.name} này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deleteProduct(product.id,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
                                loadProductsAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Lỗi xóa: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = adminProductAdapter

        saveBtn.setOnClickListener {
            val name = edtName.text.toString().trim()
            val category = if (spnCat.selectedItem != null) spnCat.selectedItem.toString() else ""
            val priceStr = edtPrice.text.toString().trim()
            val stockStr = edtStock.text.toString().trim()
            val desc = edtDesc.text.toString().trim()
            val img = selectedProductImageBase64

            if (category.isEmpty()) {
                Toast.makeText(this, "Vui lòng tạo ít nhất 1 danh mục đồ uống trước!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() || priceStr.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ Tên, Giá, Mô tả", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceStr.toDoubleOrNull() ?: 0.0
            val stock = stockStr.toIntOrNull() ?: 0

            val prodToSave = editingProduct?.copy(
                name = name,
                category = category,
                price = price,
                stockQuantity = stock,
                description = desc,
                imageUrl = img
            ) ?: Product(
                name = name,
                category = category,
                price = price,
                stockQuantity = stock,
                description = desc,
                imageUrl = img,
                available = true
            )

            if (editingProduct != null) {
                FirebaseHelper.updateProduct(prodToSave,
                    onSuccess = {
                        Toast.makeText(this, "Đã cập nhật sản phẩm!", Toast.LENGTH_SHORT).show()
                        resetEditingProductState()
                        loadProductsAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addProduct(prodToSave,
                    onSuccess = {
                        Toast.makeText(this, "Đã thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingProductState()
                        loadProductsAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun resetEditingProductState() {
        editingProduct = null
        findViewById<EditText>(R.id.edtProdName).text.clear()
        findViewById<EditText>(R.id.edtProdPrice).text.clear()
        findViewById<EditText>(R.id.edtProdStock).text.clear()
        findViewById<EditText>(R.id.edtProdDesc).text.clear()
        selectedProductImageBase64 = ""
        findViewById<TextView>(R.id.txtImageStatus).text = "Chưa chọn ảnh"
        findViewById<ImageView>(R.id.imgProductPreview).setImageResource(R.drawable.ic_drink_logo)
        val saveBtn = findViewById<Button>(R.id.btnAddProduct)
        saveBtn.text = "LƯU SẢN PHẨM"
        saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryGreen))
        findViewById<TextView>(R.id.txtProductFormTitle).text = "Thêm sản phẩm mới"
    }

    private fun loadProductsAdmin() {
        FirebaseHelper.getProducts(
            onSuccess = { adminProductAdapter.updateData(it) },
            onFailure = {}
        )
    }

    // --- 4. Quản lý Khuyến mãi (CRUD) ---
    private fun initPromosModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminPromos)
        val edtCode = findViewById<EditText>(R.id.edtPromoCodeName)
        val edtPercent = findViewById<EditText>(R.id.edtPromoDiscountPercent)
        val edtMin = findViewById<EditText>(R.id.edtPromoMinOrder)
        val saveBtn = findViewById<Button>(R.id.btnAddPromo)

        rv.layoutManager = LinearLayoutManager(this)
        promoAdapter = PromoAdapter(
            promos = emptyList(),
            onEditClick = { promo ->
                editingPromo = promo
                edtCode.setText(promo.code)
                edtCode.isEnabled = false // Không cho sửa Code vì là ID Firestore
                edtPercent.setText(promo.discountPercent.toString())
                edtMin.setText(promo.minOrderValue.toInt().toString())
                saveBtn.text = "CẬP NHẬT MÃ"
                saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBrown))
                findViewById<TextView>(R.id.txtPromoFormTitle).text = "Chỉnh sửa mã khuyến mãi"
            },
            onDeleteClick = { promo ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa mã khuyến mãi")
                    .setMessage("Bạn có chắc chắn muốn xóa mã ${promo.code} không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deletePromo(promo.code,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa mã ưu đãi thành công!", Toast.LENGTH_SHORT).show()
                                loadPromosAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = promoAdapter

        saveBtn.setOnClickListener {
            val code = edtCode.text.toString().trim().uppercase()
            val pctStr = edtPercent.text.toString().trim()
            val minStr = edtMin.text.toString().trim()

            if (code.isEmpty() || pctStr.isEmpty() || minStr.isEmpty()) {
                Toast.makeText(this, "Nhập đầy đủ thông tin mã khuyến mãi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val promoToSave = Promo(code, pctStr.toInt(), minStr.toDouble(), true)

            if (editingPromo != null) {
                FirebaseHelper.updatePromo(promoToSave,
                    onSuccess = {
                        Toast.makeText(this, "Cập nhật mã ưu đãi thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingPromoState()
                        loadPromosAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addPromo(promoToSave,
                    onSuccess = {
                        Toast.makeText(this, "Lưu mã khuyến mãi thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingPromoState()
                        loadPromosAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun resetEditingPromoState() {
        editingPromo = null
        val edtCode = findViewById<EditText>(R.id.edtPromoCodeName)
        edtCode.text.clear()
        edtCode.isEnabled = true
        findViewById<EditText>(R.id.edtPromoDiscountPercent).text.clear()
        findViewById<EditText>(R.id.edtPromoMinOrder).text.clear()
        val saveBtn = findViewById<Button>(R.id.btnAddPromo)
        saveBtn.text = "LƯU MÃ KHUYẾN MÃI"
        saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryGreen))
        findViewById<TextView>(R.id.txtPromoFormTitle).text = "Tạo mã khuyến mãi mới"
    }

    private fun loadPromosAdmin() {
        FirebaseHelper.getPromos(
            onSuccess = { promoAdapter.updateData(it) },
            onFailure = {}
        )
    }

    // --- 5. Quản lý Nhân sự & Phân quyền lọc 3 vòng tròn ---
    private fun initUsersModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminUsers)
        rv.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(
            emptyList(),
            onChangeRole = { user, newRole -> updateUserRoleAdmin(user, newRole) },
            onDeleteClick = { user -> deleteUserAdmin(user) }
        )
        rv.adapter = userAdapter

        // Bind 3 CardView vòng tròn
        val cardAdmin = findViewById<CardView>(R.id.cardFilterAdmin)
        val cardStaff = findViewById<CardView>(R.id.cardFilterStaff)
        val cardCustomer = findViewById<CardView>(R.id.cardFilterCustomer)

        // Thiết lập bộ lọc click
        cardAdmin.setOnClickListener {
            selectedRoleFilter = "ADMIN"
            filterUsersByRole()
            highlightFilterCard(cardAdmin, cardStaff, cardCustomer)
        }

        cardStaff.setOnClickListener {
            selectedRoleFilter = "STAFF"
            filterUsersByRole()
            highlightFilterCard(cardStaff, cardAdmin, cardCustomer)
        }

        cardCustomer.setOnClickListener {
            selectedRoleFilter = "CUSTOMER"
            filterUsersByRole()
            highlightFilterCard(cardCustomer, cardAdmin, cardStaff)
        }

        // Highlight mặc định Khách hàng
        highlightFilterCard(cardCustomer, cardAdmin, cardStaff)
    }

    private fun loadUsersAdmin() {
        FirebaseHelper.getAllUsers(
            onSuccess = { list ->
                allUsersList = list
                filterUsersByRole()
            },
            onFailure = {}
        )
    }

    private fun filterUsersByRole() {
        val filteredUsers = allUsersList.filter { it.role == selectedRoleFilter }
        userAdapter.updateData(filteredUsers)
    }

    private fun highlightFilterCard(activeCard: CardView, vararg inactiveCards: CardView) {
        val activeColor = when (selectedRoleFilter) {
            "ADMIN" -> R.color.primaryBrown
            "STAFF" -> R.color.primaryGreen
            else -> R.color.primaryBrownLight
        }

        activeCard.setCardBackgroundColor(ContextCompat.getColor(this, activeColor))

        for (card in inactiveCards) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white))
        }

        val imgAdmin = findViewById<ImageView>(R.id.imgFilterAdminIcon)
        val txtAdmin = findViewById<TextView>(R.id.txtFilterAdminLabel)

        val imgStaff = findViewById<ImageView>(R.id.imgFilterStaffIcon)
        val txtStaff = findViewById<TextView>(R.id.txtFilterStaffLabel)

        val imgCustomer = findViewById<ImageView>(R.id.imgFilterCustomerIcon)
        val txtCustomer = findViewById<TextView>(R.id.txtFilterCustomerLabel)

        txtAdmin.setTextColor(ContextCompat.getColor(this, R.color.primaryBrown))
        imgAdmin.setColorFilter(ContextCompat.getColor(this, R.color.primaryBrown))

        txtStaff.setTextColor(ContextCompat.getColor(this, R.color.primaryGreen))
        imgStaff.setColorFilter(ContextCompat.getColor(this, R.color.primaryGreen))

        txtCustomer.setTextColor(ContextCompat.getColor(this, R.color.grayDark))
        imgCustomer.setColorFilter(ContextCompat.getColor(this, R.color.grayDark))

        when (selectedRoleFilter) {
            "ADMIN" -> {
                txtAdmin.setTextColor(ContextCompat.getColor(this, R.color.white))
                imgAdmin.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
            "STAFF" -> {
                txtStaff.setTextColor(ContextCompat.getColor(this, R.color.white))
                imgStaff.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
            "CUSTOMER" -> {
                txtCustomer.setTextColor(ContextCompat.getColor(this, R.color.white))
                imgCustomer.setColorFilter(ContextCompat.getColor(this, R.color.white))
            }
        }
    }

    private fun updateUserRoleAdmin(user: User, newRole: String) {
        FirebaseHelper.updateUserRole(user.uid, newRole,
            onSuccess = {
                Toast.makeText(this, "Đã đổi vai trò ${user.name} thành $newRole!", Toast.LENGTH_SHORT).show()
                loadUsersAdmin()
            },
            onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun deleteUserAdmin(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Xóa người dùng")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản của ${user.name} không?")
            .setPositiveButton("Xóa") { _, _ ->
                FirebaseHelper.deleteUser(user.uid,
                    onSuccess = {
                        Toast.makeText(this, "Đã xóa người dùng thành công!", Toast.LENGTH_SHORT).show()
                        loadUsersAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // --- 6. Giám sát Đơn hàng & Lịch sử ---
    private fun initOrdersMonitorModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminOrders)
        val chkAll = findViewById<CheckBox>(R.id.chkOrdersSelectAll)
        val btnDel = findViewById<Button>(R.id.btnOrdersDeleteSelected)

        rv.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(
            orders = emptyList(),
            showActions = false,
            showSelection = true,
            onSelectionChanged = {
                updateOrdersActionBarUI()
            },
            onDeleteAction = { order ->
                deleteSingleOrderAdmin(order)
            }
        )
        rv.adapter = orderAdapter

        chkAll.setOnCheckedChangeListener { _, isChecked ->
            orderAdapter.selectAll(isChecked)
        }

        btnDel.setOnClickListener {
            deleteSelectedOrders()
        }
    }

    private fun updateOrdersActionBarUI() {
        val chkAll = findViewById<CheckBox>(R.id.chkOrdersSelectAll)
        val btnDel = findViewById<Button>(R.id.btnOrdersDeleteSelected)
        val count = orderAdapter.selectedOrderIds.size

        if (count > 0) {
            btnDel.isEnabled = true
            btnDel.text = "Xóa đã chọn ($count)"
        } else {
            btnDel.isEnabled = false
            btnDel.text = "Xóa đã chọn"
        }

        chkAll.setOnCheckedChangeListener(null)
        chkAll.isChecked = count > 0 && count == orderAdapter.itemCount
        chkAll.setOnCheckedChangeListener { _, isChecked ->
            orderAdapter.selectAll(isChecked)
        }
    }

    private fun deleteSelectedOrders() {
        val selectedIds = orderAdapter.selectedOrderIds.toList()
        if (selectedIds.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Xóa đơn hàng")
            .setMessage("Bạn có chắc chắn muốn xóa ${selectedIds.size} đơn hàng đã chọn không?")
            .setPositiveButton("Xóa") { _, _ ->
                FirebaseHelper.deleteOrdersBatch(selectedIds,
                    onSuccess = {
                        Toast.makeText(this, "Đã xóa các đơn hàng thành công!", Toast.LENGTH_SHORT).show()
                        orderAdapter.selectedOrderIds.clear()
                        updateOrdersActionBarUI()
                        loadOrdersAdmin()
                    },
                    onFailure = {
                        Toast.makeText(this, "Xóa đơn hàng thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun deleteSingleOrderAdmin(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Xóa đơn hàng")
            .setMessage("Bạn có chắc chắn muốn xóa đơn hàng #${order.id.takeLast(6).uppercase()} này không?")
            .setPositiveButton("Xóa") { _, _ ->
                FirebaseHelper.deleteOrder(order.id,
                    onSuccess = {
                        Toast.makeText(this, "Đã xóa đơn hàng thành công!", Toast.LENGTH_SHORT).show()
                        orderAdapter.selectedOrderIds.remove(order.id)
                        updateOrdersActionBarUI()
                        loadOrdersAdmin()
                    },
                    onFailure = {
                        Toast.makeText(this, "Xóa đơn hàng thất bại: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadOrdersAdmin() {
        FirebaseHelper.getAllOrdersRealtime(
            onUpdate = { orders ->
                // Lọc đơn hàng hợp lệ để Admin giám sát
                val validOrders = orders.filter { 
                    it.paymentMethod == "CASH" || it.paymentStatus == "PAID"
                }
                orderAdapter.updateData(validOrders)
                updateOrdersActionBarUI()
            },
            onFailure = {}
        )
    }

    // --- 8. Quản lý Danh mục (CRUD) ---
    private fun initCategoriesModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminCategories)
        val nameInput = findViewById<EditText>(R.id.edtCategoryName)
        val saveBtn = findViewById<Button>(R.id.btnAddCategory)

        rv.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(
            categories = emptyList(),
            onEditClick = { category ->
                editingCategory = category
                nameInput.setText(category.name)
                saveBtn.text = "CẬP NHẬT DANH MỤC"
                saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBrown))
                findViewById<TextView>(R.id.txtCategoryFormTitle).text = "Chỉnh sửa danh mục"
            },
            onDeleteClick = { category ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa danh mục")
                    .setMessage("Bạn có chắc chắn muốn xóa danh mục ${category.name}? Các sản phẩm thuộc danh mục này vẫn sẽ giữ nguyên nhưng mất liên kết.")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deleteCategory(category.id,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa danh mục thành công!", Toast.LENGTH_SHORT).show()
                                loadCategoriesAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = categoryAdapter

        saveBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categoryToSave = editingCategory?.copy(name = name) ?: Category("", name)

            if (editingCategory != null) {
                FirebaseHelper.updateCategory(categoryToSave,
                    onSuccess = {
                        Toast.makeText(this, "Cập nhật danh mục thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingCategoryState()
                        loadCategoriesAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addCategory(categoryToSave,
                    onSuccess = {
                        Toast.makeText(this, "Thêm danh mục thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingCategoryState()
                        loadCategoriesAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun resetEditingCategoryState() {
        editingCategory = null
        findViewById<EditText>(R.id.edtCategoryName).text.clear()
        val saveBtn = findViewById<Button>(R.id.btnAddCategory)
        saveBtn.text = "LƯU DANH MỤC"
        saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryGreen))
        findViewById<TextView>(R.id.txtCategoryFormTitle).text = "Thêm danh mục mới"
    }

    private fun loadCategoriesAdmin() {
        FirebaseHelper.getCategories(
            onSuccess = { categoryAdapter.updateData(it) },
            onFailure = {}
        )
    }

    // --- 9. Quản lý Banner Quảng cáo (CRUD) ---
    private fun initBannersModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminBanners)
        val contentInput = findViewById<EditText>(R.id.edtBannerContent)
        val btnSelectImg = findViewById<Button>(R.id.btnSelectBannerImage)
        val saveBtn = findViewById<Button>(R.id.btnAddBanner)

        btnSelectImg.setOnClickListener {
            imagePickerTarget = "BANNER"
            pickImageLauncher.launch("image/*")
        }

        rv.layoutManager = LinearLayoutManager(this)
        bannerAdapter = BannerAdapter(
            banners = emptyList(),
            onEditClick = { banner ->
                editingBanner = banner
                contentInput.setText(banner.content)
                selectedBannerImageBase64 = banner.imageUrl
                findViewById<TextView>(R.id.txtBannerImageStatus).text = if (banner.imageUrl.isNotEmpty()) "Ảnh hiện tại" else "Chưa có ảnh"
                FirebaseHelper.loadImage(findViewById(R.id.imgBannerPreview), selectedBannerImageBase64)

                saveBtn.text = "CẬP NHẬT BANNER"
                saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBrown))
                findViewById<TextView>(R.id.txtBannerFormTitle).text = "Chỉnh sửa banner quảng cáo"
            },
            onDeleteClick = { banner ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa quảng cáo")
                    .setMessage("Bạn có chắc chắn muốn xóa quảng cáo này không?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deleteBanner(banner.id,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa quảng cáo thành công!", Toast.LENGTH_SHORT).show()
                                loadBannersAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = bannerAdapter

        saveBtn.setOnClickListener {
            val content = contentInput.text.toString().trim()
            val img = selectedBannerImageBase64

            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung quảng cáo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val bannerToSave = editingBanner?.copy(content = content, imageUrl = img) ?: Banner("", img, content)

            if (editingBanner != null) {
                FirebaseHelper.updateBanner(bannerToSave,
                    onSuccess = {
                        Toast.makeText(this, "Cập nhật quảng cáo thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingBannerState()
                        loadBannersAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addBanner(bannerToSave,
                    onSuccess = {
                        Toast.makeText(this, "Thêm quảng cáo thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingBannerState()
                        loadBannersAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun resetEditingBannerState() {
        editingBanner = null
        findViewById<EditText>(R.id.edtBannerContent).text.clear()
        selectedBannerImageBase64 = ""
        findViewById<TextView>(R.id.txtBannerImageStatus).text = "Chưa chọn ảnh"
        findViewById<ImageView>(R.id.imgBannerPreview).setImageResource(R.drawable.ic_drink_logo)
        val saveBtn = findViewById<Button>(R.id.btnAddBanner)
        saveBtn.text = "LƯU BANNER"
        saveBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryGreen))
        findViewById<TextView>(R.id.txtBannerFormTitle).text = "Thêm banner quảng cáo mới"
    }

    private fun loadBannersAdmin() {
        FirebaseHelper.getBanners(
            onSuccess = { bannerAdapter.updateData(it) },
            onFailure = {}
        )
    }

    private fun initTablesModule() {
        val rv = findViewById<RecyclerView>(R.id.rvAdminTables)
        val numInput = findViewById<EditText>(R.id.edtTableNumber)
        val storeSpinner = findViewById<Spinner>(R.id.spnTableStore)
        val saveBtn = findViewById<Button>(R.id.btnAddTable)

        rv.layoutManager = LinearLayoutManager(this)
        tableAdapter = TableAdapter(
            tables = emptyList<Table>(),
            onViewQr = { table ->
                showQrCodeDialog(table)
            },
            onEditClick = { table ->
                editingTable = table
                numInput.setText(table.tableNumber)
                val adapter = storeSpinner.adapter as? ArrayAdapter<Store>
                if (adapter != null) {
                    for (i in 0 until adapter.count) {
                        if (adapter.getItem(i)?.id == table.storeId) {
                            storeSpinner.setSelection(i)
                            break
                        }
                    }
                }
                saveBtn.text = "CẬP NHẬT BÀN"
                findViewById<TextView>(R.id.txtTableFormTitle).text = "Chỉnh sửa thông tin bàn"
            },
            onDeleteClick = { table ->
                AlertDialog.Builder(this)
                    .setTitle("Xóa bàn")
                    .setMessage("Xóa bàn ${table.tableNumber} tại ${table.storeAddress}?")
                    .setPositiveButton("Xóa") { _, _ ->
                        FirebaseHelper.deleteTable(table.id,
                            onSuccess = {
                                Toast.makeText(this, "Đã xóa bàn!", Toast.LENGTH_SHORT).show()
                                loadTablesAdmin()
                            },
                            onFailure = { Toast.makeText(this, "Xóa lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton("Hủy", null)
                    .show()
            }
        )
        rv.adapter = tableAdapter

        saveBtn.setOnClickListener {
            val num = numInput.text.toString().trim()
            val selectedStore = storeSpinner.selectedItem as? Store

            if (num.isEmpty() || selectedStore == null) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tableToSave = editingTable?.copy(tableNumber = num, storeId = selectedStore.id, storeAddress = selectedStore.name)
                ?: Table("", selectedStore.id, selectedStore.name, num, "")
            
            if (tableToSave.qrCodeData.isEmpty()) {
                tableToSave.qrCodeData = "${selectedStore.name} - bàn $num"
            }

            if (editingTable != null) {
                FirebaseHelper.updateTable(tableToSave,
                    onSuccess = {
                        Toast.makeText(this, "Cập nhật bàn thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingTableState()
                        loadTablesAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            } else {
                FirebaseHelper.addTable(tableToSave,
                    onSuccess = {
                        Toast.makeText(this, "Thêm bàn thành công!", Toast.LENGTH_SHORT).show()
                        resetEditingTableState()
                        loadTablesAdmin()
                    },
                    onFailure = { Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun loadTablesAdmin() {
        FirebaseHelper.getTables(
            onSuccess = { list ->
                tableAdapter.updateData(list)
            },
            onFailure = { Toast.makeText(this, "Lỗi tải danh sách bàn", Toast.LENGTH_SHORT).show() }
        )
    }

    private fun loadStoresForTableSpinner() {
        FirebaseHelper.getStores(
            onSuccess = { list ->
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, list)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                findViewById<Spinner>(R.id.spnTableStore).adapter = adapter
            },
            onFailure = {}
        )
    }

    private fun resetEditingTableState() {
        editingTable = null
        findViewById<EditText>(R.id.edtTableNumber).text.clear()
        findViewById<Button>(R.id.btnAddTable).text = "LƯU THÔNG TIN BÀN"
        findViewById<TextView>(R.id.txtTableFormTitle).text = "Thêm bàn mới"
    }

    private fun showQrCodeDialog(table: Table) {
        val img = ImageView(this)
        img.setImageResource(R.drawable.ic_qr_scan)
        img.setPadding(50, 50, 50, 50)
        
        AlertDialog.Builder(this)
            .setTitle("Mã QR Bàn ${table.tableNumber}")
            .setMessage("Nội dung: ${table.qrCodeData}")
            .setView(img)
            .setPositiveButton("Đóng", null)
            .show()
    }

    // --- 10. Biểu đồ Doanh thu (Ngày, Tháng, Năm) ---
    private fun setupChartModule() {
        btnChartDay = findViewById(R.id.btnChartDay)
        btnChartMonth = findViewById(R.id.btnChartMonth)
        btnChartYear = findViewById(R.id.btnChartYear)
        layoutAdminChartBars = findViewById(R.id.layoutAdminChartBars)

        btnChartDay.setOnClickListener {
            selectedChartMode = "DAY"
            highlightChartButton()
            updateRevenueChart()
        }

        btnChartMonth.setOnClickListener {
            selectedChartMode = "MONTH"
            highlightChartButton()
            updateRevenueChart()
        }

        btnChartYear.setOnClickListener {
            selectedChartMode = "YEAR"
            highlightChartButton()
            updateRevenueChart()
        }

        highlightChartButton()
    }

    private fun highlightChartButton() {
        val activeBg = ContextCompat.getColor(this, R.color.primaryGreen)
        val activeText = ContextCompat.getColor(this, R.color.white)
        val inactiveBg = ContextCompat.getColor(this, android.R.color.transparent)
        val inactiveText = ContextCompat.getColor(this, R.color.primaryGreen)

        btnChartDay.setBackgroundResource(if (selectedChartMode == "DAY") R.drawable.bg_badge_role else 0)
        if (selectedChartMode == "DAY") btnChartDay.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBg)
        btnChartDay.setTextColor(if (selectedChartMode == "DAY") activeText else inactiveText)

        btnChartMonth.setBackgroundResource(if (selectedChartMode == "MONTH") R.drawable.bg_badge_role else 0)
        if (selectedChartMode == "MONTH") btnChartMonth.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBg)
        btnChartMonth.setTextColor(if (selectedChartMode == "MONTH") activeText else inactiveText)

        btnChartYear.setBackgroundResource(if (selectedChartMode == "YEAR") R.drawable.bg_badge_role else 0)
        if (selectedChartMode == "YEAR") btnChartYear.backgroundTintList = android.content.res.ColorStateList.valueOf(activeBg)
        btnChartYear.setTextColor(if (selectedChartMode == "YEAR") activeText else inactiveText)
    }

    private fun updateRevenueChart() {
        val completedOrders = allOrdersList.filter { it.status == "COMPLETED" }
        val sdfDay = SimpleDateFormat("dd/MM", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MM/yy", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

        val chartData = when (selectedChartMode) {
            "DAY" -> {
                // Lấy 7 ngày trước tính từ hôm nay
                val calendar = Calendar.getInstance()
                val last7Days = (0..6).map {
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.DAY_OF_YEAR, -it)
                    cal
                }.reversed()
                
                last7Days.map { cal ->
                    val dayStr = sdfDay.format(cal.time)
                    // Chỉ tính những đơn đã hoàn thành (COMPLETED) trong thống kê
                    val rev = completedOrders.filter { order ->
                        val oCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                        oCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                oCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
                    }.sumOf { it.total }
                    Pair(dayStr, rev)
                }
            }
            "MONTH" -> {
                // Lấy 6 tháng trước tính từ hôm nay
                val calendar = Calendar.getInstance()
                val last6Months = (0..5).map {
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.MONTH, -it)
                    cal
                }.reversed()

                last6Months.map { cal ->
                    val monthStr = sdfMonth.format(cal.time)
                    val rev = completedOrders.filter { order ->
                        val oCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                        oCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                                oCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                    }.sumOf { it.total }
                    Pair(monthStr, rev)
                }
            }
            "YEAR" -> {
                // Lấy 3 năm trước tính từ hôm nay
                val calendar = Calendar.getInstance()
                val last3Years = (0..2).map {
                    val cal = calendar.clone() as Calendar
                    cal.add(Calendar.YEAR, -it)
                    cal
                }.reversed()

                last3Years.map { cal ->
                    val yearStr = sdfYear.format(cal.time)
                    val rev = completedOrders.filter { order ->
                        val oCal = Calendar.getInstance().apply { timeInMillis = order.timestamp }
                        oCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                    }.sumOf { it.total }
                    Pair(yearStr, rev)
                }
            }
            else -> emptyList()
        }

        renderRevenueChart(chartData)
    }

    private fun renderRevenueChart(data: List<Pair<String, Double>>) {
        if (!::layoutAdminChartBars.isInitialized) return
        layoutAdminChartBars.removeAllViews()

        val maxVal = data.maxOfOrNull { it.second } ?: 0.0
        val maxBarHeightPx = 100 * resources.displayMetrics.density // Tối đa 100dp

        data.forEach { item ->
            val barView = LayoutInflater.from(this).inflate(R.layout.item_admin_chart_bar, layoutAdminChartBars, false)
            val txtVal = barView.findViewById<TextView>(R.id.txtChartBarValue)
            val viewVisual = barView.findViewById<View>(R.id.viewChartBarVisual)
            val txtLabel = barView.findViewById<TextView>(R.id.txtChartBarLabel)

            val rev = item.second
            val formattedValue = when {
                rev >= 1000000.0 -> String.format(Locale.US, "%.1fM", rev / 1000000.0)
                rev >= 1000.0 -> String.format(Locale.US, "%.0fk", rev / 1000.0)
                else -> String.format(Locale.US, "%.0f", rev)
            }
            txtVal.text = formattedValue
            txtLabel.text = item.first

            val height = if (maxVal > 0) {
                ((rev / maxVal) * maxBarHeightPx).toInt()
            } else {
                0
            }

            val params = viewVisual.layoutParams
            params.height = if (height > 0) height else 1
            viewVisual.layoutParams = params

            layoutAdminChartBars.addView(barView)
        }
    }

    private fun convertImageUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            val size = 250
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, size, size, true)
            
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
