package com.example.matcha_vibe.fragment

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.matcha_vibe.CartAdapter
import com.example.matcha_vibe.CartManager
import com.example.matcha_vibe.FirebaseHelper
import com.example.matcha_vibe.MainActivity
import com.example.matcha_vibe.R
import com.example.matcha_vibe.model.Order
import com.example.matcha_vibe.model.Store
import com.example.matcha_vibe.model.Table
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.zxing.integration.android.IntentIntegrator
import com.example.matcha_vibe.PayOSHelper
import android.os.Handler
import android.os.Looper
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment() {

    private lateinit var rvCartItems: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private lateinit var txtEmptyCart: TextView

    private lateinit var btnTypeDelivery: Button
    private lateinit var btnTypeDineIn: Button
    private lateinit var layoutDelivery: LinearLayout
    private lateinit var layoutDineIn: LinearLayout

    // Delivery fields
    private lateinit var spnStores: Spinner
    private lateinit var btnGetLocation: Button
    private lateinit var edtDeliveryName: EditText
    private lateinit var edtDeliveryPhone: EditText
    private lateinit var edtDeliveryAddress: EditText
    private var storesList = listOf<Store>()
    private var sortedStoresList = listOf<Store>()
    private var userLocation: Location? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Dine in fields
    private lateinit var btnScanQR: Button
    private lateinit var txtTableStatus: TextView
    private lateinit var txtTableInfoDetail: TextView
    private lateinit var edtTableAddress: EditText
    private lateinit var rgDineInPayment: RadioGroup
    private var selectedTable: Table? = null

    // Pricing fields
    private lateinit var edtPromoCode: EditText
    private lateinit var btnApplyPromo: Button
    private lateinit var txtPromoStatus: TextView
    private lateinit var txtSubtotal: TextView
    private lateinit var txtPromoDiscount: TextView
    private lateinit var txtShippingFee: TextView
    private lateinit var txtDistance: TextView
    private lateinit var txtTotalPay: TextView

    private lateinit var btnCheckout: Button

    private var orderType = "DELIVERY" // DELIVERY hoặc DINE_IN
    private val localeVN = Locale("vi", "VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)

    companion object {
        private const val LOCATION_PERMISSION_REQ_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        return inflater.inflate(R.layout.fragment_cart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nhận dữ liệu truyền từ Intent/Arguments
        arguments?.let {
            val type = it.getString("ORDER_TYPE")
            val qr = it.getString("SCANNED_QR_DATA")
            if (type == "DINE_IN") {
                orderType = "DINE_IN"
            }
            if (qr != null) {
                // Xử lý QR ngay khi fragment load
                view.post { 
                    handleScannedQrCode(qr)
                }
            }
        }

        // Bind views
        rvCartItems = view.findViewById(R.id.rvCartItems)
        txtEmptyCart = view.findViewById(R.id.txtEmptyCart)
        btnTypeDelivery = view.findViewById(R.id.btnTypeDelivery)
        btnTypeDineIn = view.findViewById(R.id.btnTypeDineIn)
        layoutDelivery = view.findViewById(R.id.layoutDeliveryDetails)
        layoutDineIn = view.findViewById(R.id.layoutDineInDetails)

        spnStores = view.findViewById(R.id.spnStores)
        btnGetLocation = view.findViewById(R.id.btnGetLocation)
        edtDeliveryName = view.findViewById(R.id.edtDeliveryName)
        edtDeliveryPhone = view.findViewById(R.id.edtDeliveryPhone)
        edtDeliveryAddress = view.findViewById(R.id.edtDeliveryAddress)

        btnScanQR = view.findViewById(R.id.btnScanQR)
        txtTableStatus = view.findViewById(R.id.txtTableStatus)
        txtTableInfoDetail = view.findViewById(R.id.txtTableInfoDetail)
        edtTableAddress = view.findViewById(R.id.edtTableAddress)
        rgDineInPayment = view.findViewById(R.id.rgDineInPayment)

        edtPromoCode = view.findViewById(R.id.edtPromoCode)
        btnApplyPromo = view.findViewById(R.id.btnApplyPromo)
        txtPromoStatus = view.findViewById(R.id.txtPromoStatus)
        txtSubtotal = view.findViewById(R.id.txtSubtotal)
        txtPromoDiscount = view.findViewById(R.id.txtPromoDiscount)
        txtShippingFee = view.findViewById(R.id.txtShippingFee)
        txtDistance = view.findViewById(R.id.txtDistance)
        txtTotalPay = view.findViewById(R.id.txtTotalPay)
        btnCheckout = view.findViewById(R.id.btnCheckout)

        // Setup Cart Recycler
        rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        cartAdapter = CartAdapter(
            cartItems = CartManager.cartList,
            onRemoveItem = { index ->
                CartManager.removeProduct(index)
                updateCartUI()
            },
            onQtyChanged = { index, newQty ->
                CartManager.updateQuantity(index, newQty)
                updateCartUI()
            }
        )
        rvCartItems.adapter = cartAdapter

        // Lấy thông tin user điền sẵn
        FirebaseHelper.getCurrentUser(
            onSuccess = { user ->
                edtDeliveryName.setText(user.name)
                edtDeliveryPhone.setText(user.phone)
            },
            onFailure = {}
        )

        // Tải danh sách cửa hàng cho Delivery
        loadStoresData()

        spnStores.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (orderType == "DELIVERY" && userLocation != null && position in sortedStoresList.indices) {
                    val store = sortedStoresList[position]
                    if (store.lat == 0.0 || store.lng == 0.0) {
                        CartManager.shippingFee = 0.0
                        txtDistance.text = "Cơ sở này chưa cập nhật tọa độ"
                        updateCartUI()
                        return
                    }

                    val storeLoc = Location("").apply {
                        latitude = store.lat
                        longitude = store.lng
                    }
                    
                    // Sử dụng cùng ROAD_FACTOR 1.25 để đồng nhất
                    val distanceKm = (userLocation!!.distanceTo(storeLoc) / 1000.0) * 1.25
                    val shippingFee = (distanceKm * 5000).coerceAtLeast(0.0)
                    
                    CartManager.shippingFee = shippingFee
                    txtDistance.visibility = View.VISIBLE
                    txtDistance.text = "Khoảng cách thực tế ước tính: %.1f km".format(distanceKm)
                    updateCartUI()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Toggle Type
        btnTypeDelivery.setOnClickListener {
            orderType = "DELIVERY"
            layoutDelivery.visibility = View.VISIBLE
            layoutDineIn.visibility = View.GONE
            highlightTypeButton(btnTypeDelivery, btnTypeDineIn)
            updateCartUI()
        }

        btnTypeDineIn.setOnClickListener {
            orderType = "DINE_IN"
            layoutDelivery.visibility = View.GONE
            layoutDineIn.visibility = View.VISIBLE
            highlightTypeButton(btnTypeDineIn, btnTypeDelivery)
            CartManager.shippingFee = 0.0
            txtDistance.visibility = View.GONE
            updateCartUI()
        }

        // Apply initial state based on orderType
        if (orderType == "DINE_IN") {
            layoutDelivery.visibility = View.GONE
            layoutDineIn.visibility = View.VISIBLE
            highlightTypeButton(btnTypeDineIn, btnTypeDelivery)
            CartManager.shippingFee = 0.0
        }

        btnGetLocation.setOnClickListener {
            checkLocationPermission()
        }

        // QR Scanning cho Dine In
        btnScanQR.setOnClickListener {
            startQrCameraScan()
        }

        // Apply Promo
        btnApplyPromo.setOnClickListener {
            val code = edtPromoCode.text.toString().trim()
            if (code.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa nhập mã", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            applyCoupon(code)
        }

        // Checkout Button
        btnCheckout.setOnClickListener {
            processCheckout()
        }

        updateCartUI()
    }

    private fun checkLocationPermission() {
        if (!isLocationEnabled()) {
            Toast.makeText(requireContext(), "Vui lòng bật Vị trí (GPS) trên thiết bị", Toast.LENGTH_LONG).show()
            startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), 
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQ_CODE)
        } else {
            getCurrentLocation()
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQ_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Quyền truy cập vị trí bị từ chối", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) return

        // Hiển thị thông báo đang lấy vị trí
        btnGetLocation.text = "Đang xác định..."
        btnGetLocation.isEnabled = false

        val cancellationTokenSource = CancellationTokenSource()
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { location ->
                btnGetLocation.text = "Lấy vị trí hiện tại"
                btnGetLocation.isEnabled = true
                if (location != null) {
                    findNearestStore(location)
                } else {
                    Toast.makeText(requireContext(), "Không thể lấy vị trí hiện tại. Hãy đảm bảo GPS đã bật và ở nơi thoáng đãng.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                btnGetLocation.text = "Lấy vị trí hiện tại"
                btnGetLocation.isEnabled = true
                Toast.makeText(requireContext(), "Lỗi định vị: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun findNearestStore(userLocation: Location) {
        this.userLocation = userLocation
        if (storesList.isEmpty()) return

        // Hệ số đường bộ (thường thực tế sẽ dài hơn đường thẳng ~25%)
        val ROAD_FACTOR = 1.25

        // Sắp xếp danh sách cửa hàng theo khoảng cách
        sortedStoresList = storesList.sortedBy { store ->
            if (store.lat == 0.0 || store.lng == 0.0) return@sortedBy Float.MAX_VALUE
            val storeLoc = Location("").apply {
                latitude = store.lat
                longitude = store.lng
            }
            userLocation.distanceTo(storeLoc)
        }

        // Tạo danh sách hiển thị kèm khoảng cách thực tế ước tính
        val displayList = sortedStoresList.map { store ->
            if (store.lat == 0.0 || store.lng == 0.0) return@map "${store.name} (Chưa có tọa độ)"
            
            val storeLoc = Location("").apply {
                latitude = store.lat
                longitude = store.lng
            }
            // Tính khoảng cách thực tế = Đường thẳng * Hệ số đường bộ
            val distKm = (userLocation.distanceTo(storeLoc) / 1000.0) * ROAD_FACTOR
            "${store.name} (~${String.format("%.1f", distKm)} km)"
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, displayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnStores.adapter = adapter

        if (sortedStoresList.isNotEmpty()) {
            spnStores.setSelection(0)
            if (edtDeliveryAddress.text.toString().isEmpty()) {
                edtDeliveryAddress.setText("Vị trí của tôi (Tọa độ: %.4f, %.4f)".format(userLocation.latitude, userLocation.longitude))
            }
        }
    }

    private fun loadStoresData() {
        FirebaseHelper.getStores(
            onSuccess = { list ->
                storesList = list
                sortedStoresList = list
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, storesList.map { it.toString() })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnStores.adapter = adapter
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Lỗi tải danh sách cửa hàng: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun highlightTypeButton(active: Button, inactive: Button) {
        active.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryGreen))
        active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        inactive.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        inactive.setTextColor(ContextCompat.getColor(requireContext(), R.color.primaryGreen))
    }

    private fun startQrCameraScan() {
        IntentIntegrator.forSupportFragment(this)
            .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            .setPrompt("Quét mã QR dán tại bàn để đặt nước")
            .setCameraId(0)
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(true)
            .initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(requireContext(), "Đã hủy quét QR", Toast.LENGTH_SHORT).show()
            } else {
                handleScannedQrCode(result.contents)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleScannedQrCode(qrData: String) {
        try {
            // Xử lý mã QR dạng text thuần: "Matcha Vibe 1 - bàn 4"
            // Hỗ trợ cả "bàn", "Bàn", "ban", "Ban"
            val regex = Regex("(.+) [-] (bàn|Bàn|ban|Ban) (\\d+)", RegexOption.IGNORE_CASE)
            val matchResult = regex.find(qrData)

            if (matchResult != null) {
                val storeName = matchResult.groupValues[1].trim()
                val tableNumber = matchResult.groupValues[3].trim()

                // Cập nhật giao diện ngay lập tức với dữ liệu quét được
                selectedTable = Table("", "", storeName, tableNumber, qrData)
                txtTableStatus.text = "Đã quét thành công: Bàn $tableNumber"
                txtTableInfoDetail.text = "Cơ sở: $storeName"
                edtTableAddress.setText("$storeName - Bàn $tableNumber")
                
                // Thử tìm kiếm trong Firebase để lấy storeId chính xác nếu cần
                FirebaseHelper.getTables(
                    onSuccess = { list ->
                        val table = list.find { 
                            (it.storeAddress.contains(storeName, ignoreCase = true) || storeName.contains(it.storeAddress, ignoreCase = true)) && 
                            it.tableNumber == tableNumber 
                        }
                        if (table != null) {
                            selectedTable = table
                            txtTableStatus.text = "Đã khớp dữ liệu: Bàn ${table.tableNumber}"
                            txtTableInfoDetail.text = "Cơ sở: ${table.storeAddress}"
                            edtTableAddress.setText("${table.storeAddress} - Bàn ${table.tableNumber}")
                        }
                    },
                    onFailure = {
                        // Nếu lỗi Firebase thì vẫn giữ dữ liệu quét trực tiếp
                    }
                )
            } else if (qrData.contains(" - ", ignoreCase = true)) {
                // Thử cách phân tách đơn giản hơn nếu regex thất bại
                val parts = qrData.split(" - ")
                if (parts.size >= 2) {
                    val storeName = parts[0].trim()
                    val tablePart = parts[1].trim()
                    // Lấy số từ phần table (ví dụ: "bàn 4" -> "4")
                    val tableNumber = tablePart.replace(Regex("[^0-9]"), "")

                    selectedTable = Table("", "", storeName, tableNumber, qrData)
                    txtTableStatus.text = "Đã quét thành công: Bàn $tableNumber"
                    txtTableInfoDetail.text = "Cơ sở: $storeName"
                    edtTableAddress.setText("$storeName - Bàn $tableNumber")
                } else {
                    handleInvalidQr(qrData)
                }
            } else {
                // Fallback cho định dạng URI (storeId=...&tableNumber=...)
                val uri = Uri.parse(qrData)
                val storeId = uri.getQueryParameter("storeId")
                val tableNumber = uri.getQueryParameter("tableNumber")
                val storeName = uri.getQueryParameter("storeName") ?: "Chi nhánh Matcha Vibe"

                if (storeId != null && tableNumber != null) {
                    FirebaseHelper.getTables(
                        onSuccess = { list ->
                            val table = list.find { it.storeId == storeId && it.tableNumber == tableNumber }
                            if (table != null) {
                                selectedTable = table
                                txtTableStatus.text = "Đã quét thành công: Bàn ${table.tableNumber}"
                                txtTableInfoDetail.text = "Cơ sở: ${table.storeAddress}"
                                edtTableAddress.setText("${table.storeAddress} - Bàn ${table.tableNumber}")
                            } else {
                                selectedTable = Table("", storeId, storeName, tableNumber, qrData)
                                txtTableStatus.text = "Quét trực tiếp: Bàn $tableNumber"
                                txtTableInfoDetail.text = "Cơ sở: $storeName"
                                edtTableAddress.setText("$storeName - Bàn $tableNumber")
                            }
                        },
                        onFailure = {
                            selectedTable = Table("", storeId, storeName, tableNumber, qrData)
                            txtTableStatus.text = "Quét trực tiếp: Bàn $tableNumber"
                            txtTableInfoDetail.text = "Cơ sở: $storeName"
                            edtTableAddress.setText("$storeName - Bàn $tableNumber")
                        }
                    )
                } else {
                    handleInvalidQr(qrData)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Lỗi giải mã QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleInvalidQr(qrData: String) {
        txtTableStatus.text = "Mã QR không đúng định dạng!"
        txtTableInfoDetail.text = "Nội dung: $qrData"
        Toast.makeText(requireContext(), "Định dạng mã QR không khớp!", Toast.LENGTH_SHORT).show()
    }

    private fun applyCoupon(code: String) {
        FirebaseHelper.getPromos(
            onSuccess = { promos ->
                val promo = promos.find { it.code.equals(code, ignoreCase = true) && it.active }
                if (promo != null) {
                    val subtotal = CartManager.getSubtotal()
                    if (subtotal >= promo.minOrderValue) {
                        CartManager.appliedPromo = promo
                        txtPromoStatus.text = "Áp dụng thành công! Giảm ${promo.discountPercent}%"
                        txtPromoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.greenAccent))
                        txtPromoStatus.visibility = View.VISIBLE
                        updateCartUI()
                    } else {
                        txtPromoStatus.text = "Đơn hàng tối thiểu phải đạt ${formatter.format(promo.minOrderValue)}"
                        txtPromoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.redAccent))
                        txtPromoStatus.visibility = View.VISIBLE
                    }
                } else {
                    txtPromoStatus.text = "Mã khuyến mãi không tồn tại hoặc đã hết hạn!"
                    txtPromoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.redAccent))
                    txtPromoStatus.visibility = View.VISIBLE
                }
            },
            onFailure = {
                Toast.makeText(requireContext(), "Lỗi kiểm tra mã: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun updateCartUI() {
        cartAdapter.notifyDataSetChanged()
        if (CartManager.cartList.isEmpty()) {
            txtEmptyCart.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
        } else {
            txtEmptyCart.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
        }

        txtSubtotal.text = formatter.format(CartManager.getSubtotal())
        txtPromoDiscount.text = "- ${formatter.format(CartManager.getDiscountAmount())}"
        
        if (orderType == "DELIVERY") {
            txtShippingFee.text = formatter.format(CartManager.shippingFee)
            (txtShippingFee.parent as View).visibility = View.VISIBLE
        } else {
            txtShippingFee.text = "0 đ"
            (txtShippingFee.parent as View).visibility = View.GONE
        }
        
        txtTotalPay.text = formatter.format(CartManager.getTotal())
    }

    private fun processCheckout() {
        if (CartManager.cartList.isEmpty()) {
            Toast.makeText(requireContext(), "Giỏ hàng trống. Vui lòng thêm sản phẩm!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUid = FirebaseHelper.getCurrentUserId() ?: return

        if (orderType == "DELIVERY") {
            val name = edtDeliveryName.text.toString().trim()
            val phone = edtDeliveryPhone.text.toString().trim()
            val address = edtDeliveryAddress.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ Tên, SĐT, Địa chỉ để giao hàng!", Toast.LENGTH_SHORT).show()
                return
            }

            if (storesList.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa có cơ sở cửa hàng để phục vụ giao!", Toast.LENGTH_SHORT).show()
                return
            }

            val store = if (sortedStoresList.isNotEmpty()) {
                sortedStoresList[spnStores.selectedItemPosition]
            } else {
                Toast.makeText(requireContext(), "Chưa chọn cơ sở cửa hàng!", Toast.LENGTH_SHORT).show()
                return
            }

            val newOrder = Order(
                userId = currentUid,
                userName = name,
                userPhone = phone,
                type = "DELIVERY",
                storeId = store.id,
                storeAddress = store.address,
                address = address,
                items = CartManager.cartList,
                subtotal = CartManager.getSubtotal(),
                discountCode = CartManager.appliedPromo?.code ?: "",
                discountAmount = CartManager.getDiscountAmount(),
                shippingFee = CartManager.shippingFee,
                total = CartManager.getTotal(),
                paymentMethod = "QR_CODE",
                paymentStatus = "UNPAID",
                status = "PENDING"
            )
            // GỌI TRỰC TIẾP THANH TOÁN THẬT, KHÔNG HIỆN DIALOG GIẢ LẬP
            submitOrderAndGetPaymentLink(newOrder, "Giao hàng: ${name} - ${phone}")

        } else {
            val table = selectedTable
            if (table == null) {
                Toast.makeText(requireContext(), "Vui lòng quét QR Bàn trước khi đặt đồ!", Toast.LENGTH_SHORT).show()
                return
            }
            val dineInAddress = edtTableAddress.text.toString()

            val isQrPay = rgDineInPayment.checkedRadioButtonId == R.id.rbPayQR

            if (isQrPay) {
                val newOrder = Order(
                    userId = currentUid,
                    userName = "Khách Bàn " + table.tableNumber,
                    type = "DINE_IN",
                    storeId = table.storeId,
                    storeAddress = table.storeAddress,
                    tableNumber = table.tableNumber,
                    address = dineInAddress,
                    items = ArrayList(CartManager.cartList),
                    subtotal = CartManager.getSubtotal(),
                    discountCode = CartManager.appliedPromo?.code ?: "",
                    discountAmount = CartManager.getDiscountAmount(),
                    shippingFee = 0.0,
                    total = CartManager.getTotal(),
                    paymentMethod = "QR_CODE",
                    paymentStatus = "UNPAID",
                    status = "PENDING"
                )
                // GỌI TRỰC TIẾP THANH TOÁN THẬT, KHÔNG HIỆN DIALOG GIẢ LẬP
                submitOrderAndGetPaymentLink(newOrder, "Đặt tại bàn ${table.tableNumber}")
            } else {
                val newOrder = Order(
                    userId = currentUid,
                    userName = "Khách Bàn " + table.tableNumber,
                    type = "DINE_IN",
                    storeId = table.storeId,
                    storeAddress = table.storeAddress,
                    tableNumber = table.tableNumber,
                    address = dineInAddress,
                    items = ArrayList(CartManager.cartList),
                    subtotal = CartManager.getSubtotal(),
                    discountCode = CartManager.appliedPromo?.code ?: "",
                    discountAmount = CartManager.getDiscountAmount(),
                    shippingFee = 0.0,
                    total = CartManager.getTotal(),
                    paymentMethod = "CASH",
                    paymentStatus = "UNPAID",
                    status = "PENDING"
                )
                submitOrderToFirestore(newOrder)
            }
        }
    }

    private fun showQrPaymentDialog(amount: Double, orderDescription: String, onPaidConfirmed: () -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_payment, null)
        val imgQR = dialogView.findViewById<ImageView>(R.id.imgQrPayment)
        val txtAmount = dialogView.findViewById<TextView>(R.id.txtQrAmount)
        val txtContent = dialogView.findViewById<TextView>(R.id.txtQrContent)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelPayment)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmPaid)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        txtAmount.text = "Số tiền: ${formatter.format(amount)}"
        val cleanContent = "MATCHAVIBE" + System.currentTimeMillis().toString().takeLast(6)
        txtContent.text = "Nội dung chuyển khoản: $cleanContent"

        val qrUrl = "https://img.vietqr.io/image/970418-109876543210-compact2.png?amount=${amount.toInt()}&addInfo=${Uri.encode(cleanContent)}&accountName=CONG%20TY%20MATCHA%20VIBE"
        
        Glide.with(this)
            .load(qrUrl)
            .placeholder(R.drawable.ic_drink_logo)
            .into(imgQR)

        btnConfirm.setOnClickListener {
            onPaidConfirmed()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun submitOrderToFirestore(order: Order) {
        btnCheckout.isEnabled = false
        btnCheckout.text = "ĐANG GỬI ĐƠN HÀNG..."

        FirebaseHelper.placeOrder(
            order = order,
            deductStock = true,
            onSuccess = { orderId ->
                Toast.makeText(requireContext(), "Đặt nước uống thành công!", Toast.LENGTH_LONG).show()
                CartManager.clearCart()
                updateCartUI()
                btnCheckout.isEnabled = true
                btnCheckout.text = "ĐẶT HÀNG NGAY"

                (activity as? MainActivity)?.loadFragment(OrdersFragment())
            },
            onFailure = { e ->
                btnCheckout.isEnabled = true
                btnCheckout.text = "ĐẶT HÀNG NGAY"
                Toast.makeText(requireContext(), "Gửi đơn hàng thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun submitOrderAndGetPaymentLink(order: Order, description: String) {
        btnCheckout.isEnabled = false
        btnCheckout.text = "ĐANG TẠO LINK THANH TOÁN..."

        // Rút gọn mô tả để không quá 25 ký tự (Yêu cầu của PayOS)
        val cleanDescription = if (description.length > 25) {
            description.substring(0, 22) + "..."
        } else {
            description
        }

        FirebaseHelper.placeOrder(
            order = order,
            deductStock = false,
            onSuccess = { orderId ->
                PayOSHelper.createPaymentLink(
                    amount = order.total.toInt(),
                    description = cleanDescription,
                    onSuccess = { checkoutUrl, orderCode ->
                        FirebaseHelper.updateOrderCode(orderId, orderCode)
                        activity?.runOnUiThread {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                            startActivity(intent)
                            CartManager.clearCart()
                            updateCartUI()
                            btnCheckout.isEnabled = true
                            btnCheckout.text = "ĐẶT HÀNG NGAY"
                            startPaymentPolling(orderId)
                            (activity as? MainActivity)?.loadFragment(OrdersFragment())
                        }
                    },
                    onFailure = { error ->
                        activity?.runOnUiThread {
                            btnCheckout.isEnabled = true
                            btnCheckout.text = "ĐẶT HÀNG NGAY"
                            Toast.makeText(requireContext(), "Lỗi tạo link: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            },
            onFailure = { e ->
                btnCheckout.isEnabled = true
                btnCheckout.text = "ĐẶT HÀNG NGAY"
                Toast.makeText(requireContext(), "Gửi đơn hàng thất bại: ${e.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun startPaymentPolling(orderId: String) {
        val handler = Handler(Looper.getMainLooper())
        val pollingRunnable = object : Runnable {
            var count = 0
            override fun run() {
                if (count > 60) return // Dừng sau 5 phút (mỗi 5s x 60)

                FirebaseHelper.getOrderStatus(orderId, { order ->
                    if (order.paymentStatus == "PAID") {
                        // Thanh toán thành công -> Thực hiện trừ kho nếu chưa trừ
                        FirebaseHelper.deductStockForOrder(orderId, {
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Thanh toán thành công và đã cập nhật kho hàng!", Toast.LENGTH_LONG).show()
                            }
                        }, { e ->
                            activity?.runOnUiThread {
                                Toast.makeText(context, "Lỗi cập nhật kho: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        })
                    } else {
                        count++
                        handler.postDelayed(this, 5000) // Kiểm tra lại sau 5 giây
                    }
                }, {})
            }
        }
        handler.post(pollingRunnable)
    }
}
