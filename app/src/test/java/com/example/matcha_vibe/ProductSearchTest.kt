package com.example.matcha_vibe

import com.example.matcha_vibe.model.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductSearchTest {

    @Test
    fun searchProduct_byName_returnsCorrectResults() {
        val products = listOf(
            Product(id = "1", name = "Matcha Latte", category = "Matcha"),
            Product(id = "2", name = "Trà Xanh Đậu Đỏ", category = "Matcha"),
            Product(id = "3", name = "Cafe Muối", category = "Coffee")
        )

        // Test 1: Tìm kiếm chính xác từ khóa "Matcha"
        val resultMatcha = products.filter { it.name.contains("Matcha", ignoreCase = true) }
        assertEquals(1, resultMatcha.size)
        assertEquals("Matcha Latte", resultMatcha[0].name)

        // Test 2: Lọc theo danh mục "Matcha"
        val resultCategory = products.filter { it.category == "Matcha" }
        assertEquals(2, resultCategory.size)
    }

    @Test
    fun searchProduct_nonExistent_returnsEmptyList() {
        val products = listOf(Product(name = "Matcha Latte"))
        
        // Test 3: Tìm một món không có trong menu
        val result = products.filter { it.name.contains("Bún đậu", ignoreCase = true) }
        
        assert(result.isEmpty())
        println("Test thành công: Không tìm thấy món 'Bún đậu' trong menu.")
    }

    @Test
    fun searchProduct_partialName_isCaseInsensitive() {
        val products = listOf(Product(name = "Cafe Muối"))
        
        // Test 4: Khách gõ chữ thường "cafe" vẫn phải ra "Cafe Muối"
        val result = products.filter { it.name.contains("cafe", ignoreCase = true) }
        
        assertEquals(1, result.size)
        assertEquals("Cafe Muối", result[0].name)
        println("Test thành công: Tìm kiếm không phân biệt hoa thường hoạt động tốt.")
    }
}
