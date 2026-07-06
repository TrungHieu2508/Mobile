package com.example.matcha_vibe

import com.example.matcha_vibe.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.bumptech.glide.Glide

object FirebaseHelper {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    // --- Authentication ---
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUser(onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        val uid = getCurrentUserId()
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        onSuccess(user)
                    } else {
                        onFailure(Exception("Không tìm thấy thông tin người dùng"))
                    }
                }
                .addOnFailureListener {
                    onFailure(it)
                }
        } else {
            onFailure(Exception("Chưa đăng nhập"))
        }
    }

    fun login(email: String, password: String, onSuccess: (User) -> Unit, onFailure: (Exception) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val user = doc.toObject(User::class.java)
                            if (user != null) {
                                onSuccess(user)
                            } else {
                                // Nếu không có trong database, tạo mới mặc định CUSTOMER
                                val newUser = User(uid, authResult.user?.displayName ?: "Người dùng", email, "", "CUSTOMER")
                                db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener { onSuccess(newUser) }
                                    .addOnFailureListener { onFailure(it) }
                            }
                        }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("UID null"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun register(user: User, password: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid
                if (uid != null) {
                    user.uid = uid
                    db.collection("users").document(uid).set(user)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                } else {
                    onFailure(Exception("Đăng ký thành công nhưng lỗi UID"))
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun logout() {
        auth.signOut()
    }

    // --- Products ---
    fun getProducts(onSuccess: (List<Product>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("products")
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Product::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addProduct(product: Product, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = db.collection("products").document()
        product.id = ref.id
        ref.set(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateProduct(product: Product, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("products").document(product.id).set(product)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteProduct(productId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("products").document(productId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateProductStock(productId: String, quantityToSubtract: Int) {
        val ref = db.collection("products").document(productId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)
            val currentStock = snapshot.getLong("stockQuantity") ?: 0
            val newStock = (currentStock - quantityToSubtract).coerceAtLeast(0)
            transaction.update(ref, "stockQuantity", newStock)
            null
        }
    }

    // --- Stores ---
    fun getStores(onSuccess: (List<Store>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("stores")
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<Store>()
                for (doc in result) {
                    try {
                        val id = doc.id
                        val name = doc.getString("name") ?: "Cơ sở không tên"
                        val address = doc.getString("address") ?: "Chưa có địa chỉ"
                        
                        // Lấy Lat/Lng linh hoạt: chấp nhận Double, Long, String hoặc mặc định 0.0
                        val lat = when (val rawLat = doc.get("lat")) {
                            is Number -> rawLat.toDouble()
                            is String -> rawLat.replace(",", ".").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        
                        val lng = when (val rawLng = doc.get("lng")) {
                            is Number -> rawLng.toDouble()
                            is String -> rawLng.replace(",", ".").toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        
                        list.add(Store(id, name, address, lat, lng))
                    } catch (e: Exception) {
                        // Nếu bản ghi lỗi nặng, vẫn hiện để Admin có thể xóa hoặc sửa lại
                        list.add(Store(doc.id, "Dữ liệu lỗi (${doc.id})", "Bấm sửa để nhập lại", 0.0, 0.0))
                    }
                }
                // Sắp xếp theo tên để dễ quản lý
                val sortedList = list.sortedBy { it.name }
                onSuccess(sortedList)
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun addStore(store: Store, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = db.collection("stores").document()
        store.id = ref.id
        // Đảm bảo lưu đúng kiểu số Double cho tọa độ
        val storeData = mapOf(
            "id" to store.id,
            "name" to store.name,
            "address" to store.address,
            "lat" to store.lat,
            "lng" to store.lng
        )
        ref.set(storeData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateStore(store: Store, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val storeData = mapOf(
            "id" to store.id,
            "name" to store.name,
            "address" to store.address,
            "lat" to store.lat,
            "lng" to store.lng
        )
        db.collection("stores").document(store.id).set(storeData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteStore(storeId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("stores").document(storeId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Tables ---
    fun getTables(onSuccess: (List<Table>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("tables").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Table::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addTable(table: Table, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = db.collection("tables").document()
        table.id = ref.id
        // QR Code data: matchavibe://table?storeId=XYZ&tableNumber=5
        table.qrCodeData = "matchavibe://table?storeId=${table.storeId}&tableNumber=${table.tableNumber}"
        ref.set(table)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateTable(table: Table, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        table.qrCodeData = "matchavibe://table?storeId=${table.storeId}&tableNumber=${table.tableNumber}"
        db.collection("tables").document(table.id).set(table)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteTable(tableId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("tables").document(tableId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Promos ---
    fun getPromos(onSuccess: (List<Promo>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("promos").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Promo::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addPromo(promo: Promo, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("promos").document(promo.code).set(promo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updatePromo(promo: Promo, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("promos").document(promo.code).set(promo)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deletePromo(promoCode: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("promos").document(promoCode).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Orders ---
    fun placeOrder(order: Order, deductStock: Boolean, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = db.collection("orders").document()
        order.id = orderRef.id
        order.stockDeducted = deductStock

        db.runTransaction { transaction ->
            if (deductStock) {
                // 1. Kiểm tra và trừ kho cho từng sản phẩm
                for (item in order.items) {
                    val productRef = db.collection("products").document(item.productId)
                    val productSnap = transaction.get(productRef)
                    val currentStock = productSnap.getLong("stockQuantity") ?: 0
                    
                    if (currentStock < item.quantity) {
                        throw Exception("Sản phẩm ${item.productName} không đủ số lượng trong kho (Còn: $currentStock)")
                    }
                    
                    transaction.update(productRef, "stockQuantity", currentStock - item.quantity)
                }
            }

            // 2. Lưu đơn hàng
            transaction.set(orderRef, order)
            null
        }.addOnSuccessListener {
            onSuccess(order.id)
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    fun deductStockForOrder(orderId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val orderRef = db.collection("orders").document(orderId)
        
        db.runTransaction { transaction ->
            val orderSnap = transaction.get(orderRef)
            val order = orderSnap.toObject(Order::class.java) ?: throw Exception("Không tìm thấy đơn hàng")
            
            if (order.stockDeducted) return@runTransaction null // Đã trừ rồi thì thôi

            // Trừ kho cho từng sản phẩm trong đơn
            for (item in order.items) {
                val productRef = db.collection("products").document(item.productId)
                val productSnap = transaction.get(productRef)
                val currentStock = productSnap.getLong("stockQuantity") ?: 0
                
                // Lưu ý: Lúc này có thể kho đã hết do người khác mua trước
                if (currentStock < item.quantity) {
                    throw Exception("Sản phẩm ${item.productName} đã hết hàng trong lúc bạn thanh toán!")
                }
                
                transaction.update(productRef, "stockQuantity", currentStock - item.quantity)
            }
            
            // Đánh dấu đã trừ kho
            transaction.update(orderRef, "stockDeducted", true)
            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener {
            onFailure(it)
        }
    }

    fun getOrdersForUser(userId: String, onSuccess: (List<Order>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Order::class.java) }
                val sortedList = list.sortedByDescending { it.timestamp }
                onSuccess(sortedList)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getAllOrdersRealtime(onUpdate: (List<Order>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("orders")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.mapNotNull { it.toObject(Order::class.java) }
                    onUpdate(list)
                }
            }
    }

    fun updateOrderStatus(orderId: String, status: String, paymentStatus: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("orders").document(orderId)
            .update("status", status, "paymentStatus", paymentStatus)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteOrder(orderId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("orders").document(orderId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteOrdersBatch(orderIds: List<String>, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val batch = db.batch()
        for (id in orderIds) {
            val ref = db.collection("orders").document(id)
            batch.delete(ref)
        }
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateOrderCode(orderId: String, orderCode: Long) {
        db.collection("orders").document(orderId)
            .update("orderCode", orderCode)
    }

    fun getOrderStatus(orderId: String, onSuccess: (Order) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("orders").document(orderId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onFailure(e)
                    return@addSnapshotListener
                }
                val order = snapshot?.toObject(Order::class.java)
                if (order != null) {
                    onSuccess(order)
                }
            }
    }


    // --- User Management (Admin) ---
    fun getAllUsers(onSuccess: (List<User>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(User::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateUserRole(userId: String, role: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId)
            .update("role", role)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Categories ---
    fun getCategories(onSuccess: (List<Category>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("categories").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Category::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addCategory(category: Category, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = db.collection("categories").document()
        category.id = ref.id
        ref.set(category)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateCategory(category: Category, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("categories").document(category.id).set(category)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteCategory(categoryId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("categories").document(categoryId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // --- Banners ---
    fun getBanners(onSuccess: (List<Banner>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("banners").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { it.toObject(Banner::class.java) }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addBanner(banner: Banner, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val ref = db.collection("banners").document()
        banner.id = ref.id
        ref.set(banner)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateBanner(banner: Banner, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("banners").document(banner.id).set(banner)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteBanner(bannerId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("banners").document(bannerId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


    fun updateUserProfile(userId: String, name: String, phone: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("users").document(userId)
            .update("name", name, "phone", phone)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val user = auth.currentUser
        val email = user?.email
        if (user != null && email != null) {
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it) }
                }
                .addOnFailureListener { onFailure(it) }
        } else {
            onFailure(Exception("Người dùng chưa đăng nhập hoặc không hợp lệ"))
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loadImage(view: android.widget.ImageView, url: String) {
        val ctx = view.context
        if (url.isNotEmpty()) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Glide.with(ctx)
                    .load(url)
                    .placeholder(R.drawable.app_logo)
                    .error(R.drawable.app_logo)
                    .into(view)
            } else {
                try {
                    val cleanBase64 = if (url.contains(",")) url.substringAfter(",") else url
                    val imageBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    Glide.with(ctx)
                        .load(imageBytes)
                        .placeholder(R.drawable.app_logo)
                        .error(R.drawable.app_logo)
                        .into(view)
                } catch (e: Exception) {
                    view.setImageResource(R.drawable.app_logo)
                }
            }
        } else {
            view.setImageResource(R.drawable.app_logo)
        }
    }
}
