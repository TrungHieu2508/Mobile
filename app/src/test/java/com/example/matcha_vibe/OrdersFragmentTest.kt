package com.example.matcha_vibe

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.matcha_vibe.model.Order
import com.google.firebase.firestore.*
import io.mockk.*
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Robolectric Local UI Test cho chức năng Lịch sử Đơn hàng.
 * Cấu hình màn hình rộng (xhdpi) để tránh bị che khuất các nút bấm.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w1200dp-h1600dp-xhdpi")
class OrdersFragmentTest {

    private val mockFirestore = mockk<FirebaseFirestore>(relaxed = true)
    private val mockCollection = mockk<CollectionReference>(relaxed = true)
    private val mockQuery = mockk<Query>(relaxed = true)
    private val mockSnapshot = mockk<QuerySnapshot>(relaxed = true)

    private val snapshotListenerSlot = slot<EventListener<QuerySnapshot>>()

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        mockkObject(FirebaseHelper)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic(com.google.firebase.auth.FirebaseAuth::class)

        every { FirebaseHelper.getCurrentUserId() } returns "test_user_id"
        
        val mockAuth = mockk<com.google.firebase.auth.FirebaseAuth>(relaxed = true)
        every { com.google.firebase.auth.FirebaseAuth.getInstance() } returns mockAuth

        every { FirebaseFirestore.getInstance() } returns mockFirestore
        every { mockFirestore.collection("orders") } returns mockCollection
        every { mockCollection.whereEqualTo("userId", "test_user_id") } returns mockQuery
        every { mockQuery.addSnapshotListener(capture(snapshotListenerSlot)) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun loadUserOrders_emptyList_showsEmptyMessage() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_orders)).perform(click())

        every { mockSnapshot.isEmpty } returns true
        every { mockSnapshot.iterator() } returns mutableListOf<QueryDocumentSnapshot>().iterator()

        if (snapshotListenerSlot.isCaptured) {
            snapshotListenerSlot.captured.onEvent(mockSnapshot, null)
        }

        onView(withId(R.id.txtNoOrders)).check(matches(isDisplayed()))
        onView(withId(R.id.rvOrders)).check(matches(withEffectiveVisibility(Visibility.GONE)))

        scenario.close()
    }

    @Test
    fun loadUserOrders_hasData_displaysOrderInfo() {
        val testOrder = Order(
            id = "ORDER_DER001",
            total = 85000.0,
            status = "PENDING",
            paymentMethod = "CASH",
            paymentStatus = "UNPAID",
            timestamp = System.currentTimeMillis()
        )

        val mockDoc = mockk<QueryDocumentSnapshot>()
        every { mockDoc.toObject(Order::class.java) } returns testOrder
        every { mockSnapshot.iterator() } answers { mutableListOf(mockDoc).iterator() }
        every { mockSnapshot.isEmpty } returns false

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_orders)).perform(click())

        if (snapshotListenerSlot.isCaptured) {
            snapshotListenerSlot.captured.onEvent(mockSnapshot, null)
        }

        onView(withText(containsString("DER001"))).check(matches(isDisplayed()))
        onView(withText(containsString("85.000"))).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun orderStatus_logic_displaysCorrectBadgeText() {
        val pendingOrder = Order(id = "ID1", status = "PENDING", paymentMethod = "CASH", timestamp = 2000)
        val deliveringOrder = Order(id = "ID2", status = "DELIVERING", paymentMethod = "CASH", timestamp = 1000)

        val doc1 = mockk<QueryDocumentSnapshot>(); every { doc1.toObject(Order::class.java) } returns pendingOrder
        val doc2 = mockk<QueryDocumentSnapshot>(); every { doc2.toObject(Order::class.java) } returns deliveringOrder

        every { mockSnapshot.iterator() } answers { mutableListOf(doc1, doc2).iterator() }
        every { mockSnapshot.isEmpty } returns false

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_orders)).perform(click())
        
        if (snapshotListenerSlot.isCaptured) {
            snapshotListenerSlot.captured.onEvent(mockSnapshot, null)
        }

        onView(withText("ĐANG CHỜ")).check(matches(isDisplayed()))
        onView(withText("ĐANG GIAO ĐỒ")).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun interaction_clickCancel_opensConfirmationDialog() {
        // Given: Một đơn hàng hợp lệ ở trạng thái PENDING (để có nút Hủy)
        val testOrder = Order(id = "ID_CANCEL", status = "PENDING", paymentMethod = "CASH")
        val doc = mockk<QueryDocumentSnapshot>(); every { doc.toObject(Order::class.java) } returns testOrder
        
        every { mockSnapshot.iterator() } answers { mutableListOf(doc).iterator() }
        every { mockSnapshot.isEmpty } returns false

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_orders)).perform(click())
        
        if (snapshotListenerSlot.isCaptured) {
            snapshotListenerSlot.captured.onEvent(mockSnapshot, null)
        }

        // When: Click vào nút Hủy đơn hàng
        onView(withId(R.id.btnOrderActionCancel)).perform(click())
        
        // Buộc Robolectric xử lý hết các message trong queue để Dialog kịp hiện lên
        ShadowLooper.idleMainLooper()

        // Then: Kiểm tra nội dung Dialog xuất hiện trong cửa sổ Root là Dialog
        // Kiểm tra tiêu đề
        onView(withText("Hủy đơn hàng"))
            .inRoot(isDialog()) 
            .check(matches(isDisplayed()))
            
        // Kiểm tra thông điệp xác nhận
        onView(withText(containsString("chắc chắn muốn hủy đơn hàng")))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        scenario.close()
    }
}
