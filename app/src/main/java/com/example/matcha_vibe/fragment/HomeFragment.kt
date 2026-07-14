package com.example.matcha_vibe.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.matcha_vibe.*
import com.example.matcha_vibe.model.Banner
import com.example.matcha_vibe.model.Product
import java.util.Timer
import java.util.TimerTask

class HomeFragment : Fragment() {

    private lateinit var vpBanners: ViewPager2
    private lateinit var rvAllDrinksRunning: RecyclerView
    private lateinit var layoutDynamicCategorySections: LinearLayout
    private lateinit var layoutHomeStores: LinearLayout
    private lateinit var edtSearch: EditText

    private lateinit var homeBannerAdapter: HomeBannerAdapter
    private lateinit var productMiniAdapter: ProductMiniAdapter

    private var allProductsList = listOf<Product>()
    private var bannerTimer: Timer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        val txtGreeting = view.findViewById<TextView>(R.id.txtGreeting)
        edtSearch = view.findViewById(R.id.edtHomeSearch)
        vpBanners = view.findViewById(R.id.vpHomeBanners)
        rvAllDrinksRunning = view.findViewById(R.id.rvAllDrinksRunning)
        layoutDynamicCategorySections = view.findViewById(R.id.layoutDynamicCategorySections)
        layoutHomeStores = view.findViewById(R.id.layoutHomeStores)
        val txtHomeViewMenu = view.findViewById<TextView>(R.id.txtHomeViewMenu)

        // Setup Banners
        homeBannerAdapter = HomeBannerAdapter(emptyList())
        vpBanners.adapter = homeBannerAdapter

        // Setup Mini Running list
        rvAllDrinksRunning.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        productMiniAdapter = ProductMiniAdapter(emptyList()) { product ->
            showProductDetail(product)
        }
        rvAllDrinksRunning.adapter = productMiniAdapter

        // Lấy thông tin người dùng chào hỏi
        FirebaseHelper.getCurrentUser(
            onSuccess = { user ->
                txtGreeting.text = "Xin chào, ${user.name}!"
            },
            onFailure = {
                txtGreeting.text = "Xin chào, Bạn!"
            }
        )

        // Load data
        loadBannersHome()
        loadProductsHome()
        loadStoresHome()

        // Search text watcher
        edtSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProductsHome(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Click View Menu to go to OrderActivity
        txtHomeViewMenu.setOnClickListener {
            openOrderActivity("Tất cả")
        }
    }

    private fun loadBannersHome() {
        FirebaseHelper.getBanners(
            onSuccess = { list ->
                homeBannerAdapter.updateData(list)
                setupBannerAutoScroll(list.size)
            },
            onFailure = {}
        )
    }

    private fun setupBannerAutoScroll(size: Int) {
        if (size <= 1) return
        bannerTimer?.cancel()
        bannerTimer = Timer()
        bannerTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    if (isAdded && view != null) {
                        val nextItem = (vpBanners.currentItem + 1) % size
                        vpBanners.setCurrentItem(nextItem, true)
                    }
                }
            }
        }, 4000, 4000)
    }

    private fun loadProductsHome() {
        FirebaseHelper.getProducts(
            onSuccess = { list ->
                allProductsList = list.filter { it.available }
                productMiniAdapter.updateData(allProductsList)
                loadDynamicCategoriesWithProducts(allProductsList)
            },
            onFailure = { e ->
                Toast.makeText(requireContext(), "Lỗi tải sản phẩm: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun loadDynamicCategoriesWithProducts(products: List<Product>) {
        FirebaseHelper.getCategories(
            onSuccess = { categories ->
                if (!isAdded) return@getCategories
                layoutDynamicCategorySections.removeAllViews()
                categories.forEach { category ->
                    val categoryProducts = products.filter { it.category.equals(category.name, ignoreCase = true) }
                    if (categoryProducts.isNotEmpty()) {
                        val sectionView = LayoutInflater.from(requireContext()).inflate(R.layout.item_category_section, layoutDynamicCategorySections, false)
                        val txtTitle = sectionView.findViewById<TextView>(R.id.txtCategorySectionTitle)
                        val txtViewAll = sectionView.findViewById<TextView>(R.id.txtCategorySectionViewAll)
                        val rvProducts = sectionView.findViewById<RecyclerView>(R.id.rvCategorySectionProducts)

                        txtTitle.text = category.name
                        txtViewAll.setOnClickListener {
                            openOrderActivity(category.name)
                        }

                        rvProducts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                        val horizontalAdapter = ProductHorizontalAdapter(
                            products = categoryProducts,
                            onItemClick = { product -> showProductDetail(product) },
                            onAddClick = { product -> showProductDetail(product) }
                        )
                        rvProducts.adapter = horizontalAdapter

                        layoutDynamicCategorySections.addView(sectionView)
                    }
                }
            },
            onFailure = {}
        )
    }

    private fun filterProductsHome(query: String) {
        val filtered = if (query.isEmpty()) {
            allProductsList
        } else {
            allProductsList.filter { it.name.contains(query, ignoreCase = true) }
        }
        productMiniAdapter.updateData(filtered)
        loadDynamicCategoriesWithProducts(filtered)
    }

    private fun loadStoresHome() {
        FirebaseHelper.getStores(
            onSuccess = { list ->
                if (!isAdded) return@getStores
                layoutHomeStores.removeAllViews()
                list.forEach { store ->
                    val storeView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, layoutHomeStores, false)
                    val txtName = storeView.findViewById<TextView>(android.R.id.text1)
                    val txtAddress = storeView.findViewById<TextView>(android.R.id.text2)

                    txtName.text = store.name
                    txtName.setTextColor(ContextCompat.getColor(requireContext(), R.color.primaryBrown))
                    txtName.setTypeface(null, android.graphics.Typeface.BOLD)

                    txtAddress.text = store.address
                    txtAddress.setTextColor(ContextCompat.getColor(requireContext(), R.color.grayDark))
                    txtAddress.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)

                    val divider = View(requireContext())
                    val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)
                    lp.setMargins(0, 8, 0, 8)
                    divider.layoutParams = lp
                    divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primaryGreenLight))

                    layoutHomeStores.addView(storeView)
                    layoutHomeStores.addView(divider)
                }
                // Xóa divider cuối cùng nếu có
                if (layoutHomeStores.childCount > 0) {
                    layoutHomeStores.removeViewAt(layoutHomeStores.childCount - 1)
                }
            },
            onFailure = {}
        )
    }

    private fun showProductDetail(product: Product) {
        val bottomSheet = ProductDetailBottomSheet(product) {
            // Callback khi thêm thành công vào giỏ
        }
        bottomSheet.show(childFragmentManager, "ProductDetailBottomSheet")
    }

    private fun openOrderActivity(categoryName: String) {
        val intent = Intent(requireContext(), OrderActivity::class.java).apply {
            putExtra("SELECTED_CATEGORY", categoryName)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bannerTimer?.cancel()
    }
}
