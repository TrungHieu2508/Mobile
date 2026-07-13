package com.example.matcha_vibe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.matcha_vibe.model.Product
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.NumberFormat
import java.util.Locale

class ProductDetailBottomSheet(
    private val product: Product,
    private val onAdded: () -> Unit
) : BottomSheetDialogFragment() {

    private var quantity = 1
    private val localeVN = Locale("vi", "VN")
    private val formatter = NumberFormat.getCurrencyInstance(localeVN)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_product_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txtName = view.findViewById<TextView>(R.id.txtDetailName)
        val txtPrice = view.findViewById<TextView>(R.id.txtDetailPrice)
        val txtDesc = view.findViewById<TextView>(R.id.txtDetailDesc)
        
        val txtLabelSize = view.findViewById<TextView>(R.id.txtLabelSize)
        val rgSize = view.findViewById<RadioGroup>(R.id.rgSize)
        
        val txtLabelSugar = view.findViewById<TextView>(R.id.txtLabelSugar)
        val rgSugar = view.findViewById<RadioGroup>(R.id.rgSugar)
        
        val txtLabelIce = view.findViewById<TextView>(R.id.txtLabelIce)
        val rgIce = view.findViewById<RadioGroup>(R.id.rgIce)

        val txtLabelNote = view.findViewById<TextView>(R.id.txtLabelNote)
        val edtNote = view.findViewById<EditText>(R.id.edtSpecialNote)
        
        val btnMinus = view.findViewById<Button>(R.id.btnQtyMinus)
        val btnPlus = view.findViewById<Button>(R.id.btnQtyPlus)
        val txtCount = view.findViewById<TextView>(R.id.txtQtyCount)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmitAddToCart)

        // Set thông tin cơ bản
        txtName.text = product.name
        txtDesc.text = "${product.description}\n(Kho còn: ${product.stockQuantity})"
        
        if (product.stockQuantity <= 0) {
            btnSubmit.isEnabled = false
            btnSubmit.text = "HẾT HÀNG"
            btnSubmit.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.grayMedium))
        }

        // Kiểm tra xem sản phẩm có phải là bánh không
        val isCake = isCakeProduct()
        
        if (isCake) {
            // Ẩn các phần chọn size, đường, đá, ghi chú cho bánh
            txtLabelSize.visibility = View.GONE
            rgSize.visibility = View.GONE
            txtLabelSugar.visibility = View.GONE
            rgSugar.visibility = View.GONE
            txtLabelIce.visibility = View.GONE
            rgIce.visibility = View.GONE
            txtLabelNote.visibility = View.GONE
            edtNote.visibility = View.GONE
        }

        updatePriceText(txtPrice, rgSize)

        // Theo dõi thay đổi kích cỡ để cập nhật giá hiển thị ngay lập tức
        rgSize.setOnCheckedChangeListener { _, _ ->
            updatePriceText(txtPrice, rgSize)
        }

        // Tăng giảm số lượng
        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                txtCount.text = quantity.toString()
                updatePriceText(txtPrice, rgSize)
            }
        }

        btnPlus.setOnClickListener {
            quantity++
            txtCount.text = quantity.toString()
            updatePriceText(txtPrice, rgSize)
        }

        // Click thêm vào giỏ
        btnSubmit.setOnClickListener {
            // Kiểm tra số lượng tồn kho trước khi thêm
            if (quantity > product.stockQuantity) {
                Toast.makeText(requireContext(), "Rất tiếc, chỉ còn ${product.stockQuantity} sản phẩm trong kho!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val size: String
            val sugar: String
            val ice: String
            val note: String

            if (!isCake) {
                // Lấy size đã chọn cho nước
                val selectedSizeId = rgSize.checkedRadioButtonId
                size = when (selectedSizeId) {
                    R.id.rbSizeS -> "S"
                    R.id.rbSizeL -> "L"
                    else -> "M"
                }

                // Lấy mức đường đã chọn cho nước
                val selectedSugarId = rgSugar.checkedRadioButtonId
                sugar = when (selectedSugarId) {
                    R.id.rbSugar0 -> "0%"
                    R.id.rbSugar50 -> "50%"
                    else -> "100%"
                }

                // Lấy mức đá đã chọn cho nước
                val selectedIceId = rgIce.checkedRadioButtonId
                ice = when (selectedIceId) {
                    R.id.rbIce0 -> "0%"
                    R.id.rbIce50 -> "50%"
                    else -> "100%"
                }
                note = edtNote.text.toString().trim()
            } else {
                // Đối với bánh, mặc định là các giá trị trống hoặc không áp dụng
                size = ""
                sugar = ""
                ice = ""
                note = ""
            }

            // Thêm vào giỏ thông qua CartManager
            CartManager.addProduct(product, size, sugar, ice, quantity, note)

            Toast.makeText(requireContext(), "Đã thêm ${quantity}x ${product.name} vào giỏ hàng", Toast.LENGTH_SHORT).show()
            onAdded()
            dismiss()
        }
    }

    private fun isCakeProduct(): Boolean {
        val category = product.category.lowercase()
        return category.contains("bánh") || category.contains("cake")
    }

    private fun updatePriceText(txtPrice: TextView, rgSize: RadioGroup) {
        val extraPrice = when (rgSize.checkedRadioButtonId) {
            R.id.rbSizeS -> -5000.0
            R.id.rbSizeL -> 5000.0
            else -> 0.0
        }
        val finalPrice = (product.price + extraPrice) * quantity
        txtPrice.text = formatter.format(finalPrice)
    }
}
