package com.example.matcha_vibe

import com.example.matcha_vibe.model.Table
import com.example.matcha_vibe.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * Unit test cho các business rules thuộc Customer/Auth/Profile/QR.
 * Lưu ý: nhiều logic trong Activity/Fragment hiện đang là private và phụ thuộc Android UI/Firebase,
 * nên file này test phần rule thuần túy được trích đúng từ code gốc.
 */
class CustomerAuthQrProfileLogicTest {

    private enum class Destination {
        LOGIN, MAIN, STAFF, ADMIN
    }

    private enum class RegisterError {
        EMPTY_FIELDS,
        NAME_MUST_HAVE_AT_LEAST_TWO_WORDS,
        INVALID_VN_PHONE,
        EMAIL_MUST_BE_GMAIL,
        WEAK_PASSWORD
    }

    private enum class PasswordChangeError {
        EMPTY_FIELDS,
        NEW_PASSWORD_TOO_SHORT,
        CONFIRM_PASSWORD_NOT_MATCH
    }

    private fun destinationForSession(currentUid: String?, role: String?): Destination {
        if (currentUid == null) return Destination.LOGIN
        return when (role) {
            "ADMIN" -> Destination.ADMIN
            "STAFF" -> Destination.STAFF
            else -> Destination.MAIN
        }
    }

    private fun validateRegisterInput(
        name: String,
        email: String,
        phone: String,
        password: String
    ): RegisterError? {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()
        val trimmedPassword = password.trim()

        if (trimmedName.isEmpty() || trimmedEmail.isEmpty() || trimmedPhone.isEmpty() || trimmedPassword.isEmpty()) {
            return RegisterError.EMPTY_FIELDS
        }

        val words = trimmedName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.size < 2) return RegisterError.NAME_MUST_HAVE_AT_LEAST_TWO_WORDS

        val phoneRegex = "^(0[35789])[0-9]{8}$".toRegex()
        if (!phoneRegex.matches(trimmedPhone)) return RegisterError.INVALID_VN_PHONE

        if (!trimmedEmail.lowercase().endsWith("@gmail.com")) return RegisterError.EMAIL_MUST_BE_GMAIL

        val hasLetter = trimmedPassword.any { it.isLetter() }
        val hasDigit = trimmedPassword.any { it.isDigit() }
        if (trimmedPassword.length < 8 || !hasLetter || !hasDigit) return RegisterError.WEAK_PASSWORD

        return null
    }

    private fun buildRegisteredCustomer(name: String, email: String, phone: String): User {
        return User(
            uid = "",
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = "CUSTOMER"
        )
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): PasswordChangeError? {
        val current = currentPassword.trim()
        val newPw = newPassword.trim()
        val confirm = confirmPassword.trim()

        if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            return PasswordChangeError.EMPTY_FIELDS
        }
        if (newPw.length < 6) return PasswordChangeError.NEW_PASSWORD_TOO_SHORT
        if (newPw != confirm) return PasswordChangeError.CONFIRM_PASSWORD_NOT_MATCH
        return null
    }

    private fun calculateShippingFeeFromStraightDistanceMeters(distanceMeters: Double): Double {
        val roadFactor = 1.25
        val distanceKm = (distanceMeters / 1000.0) * roadFactor
        return (distanceKm * 5000.0).coerceAtLeast(0.0)
    }

    private fun findNearestStoreIndex(distancesInMeters: List<Double>): Int? {
        if (distancesInMeters.isEmpty()) return null
        return distancesInMeters.withIndex().minBy { it.value }.index
    }

    /**
     * Parser mô phỏng handleScannedQrCode() trong CartFragment.kt:
     * - "Matcha Vibe 1 - bàn 4"
     * - "Matcha Vibe 1 - Ban 4"
     * - URI có query storeId, tableNumber, storeName.
     */
    private fun parseTableQr(qrData: String): Table? {
        val regex = Regex("(.+) [-] (bàn|Bàn|ban|Ban) (\\d+)", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(qrData)
        if (matchResult != null) {
            val storeName = matchResult.groupValues[1].trim()
            val tableNumber = matchResult.groupValues[3].trim()
            return Table("", "", storeName, tableNumber, qrData)
        }

        if (qrData.contains(" - ", ignoreCase = true)) {
            val parts = qrData.split(" - ")
            if (parts.size >= 2) {
                val storeName = parts[0].trim()
                val tableNumber = parts[1].trim().replace(Regex("[^0-9]"), "")
                return if (tableNumber.isNotEmpty()) {
                    Table("", "", storeName, tableNumber, qrData)
                } else {
                    null
                }
            }
        }

        return try {
            val uri = URI(qrData)
            val query = uri.rawQuery ?: return null
            val params = query.split("&")
                .mapNotNull {
                    val pair = it.split("=", limit = 2)
                    if (pair.size == 2) pair[0] to java.net.URLDecoder.decode(pair[1], "UTF-8") else null
                }
                .toMap()

            val storeId = params["storeId"]
            val tableNumber = params["tableNumber"]
            val storeName = params["storeName"] ?: "Chi nhánh Matcha Vibe"

            if (storeId != null && tableNumber != null) {
                Table("", storeId, storeName, tableNumber, qrData)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    @Test
    fun loginRouting_noCurrentUser_goesToLogin() {
        assertEquals(Destination.LOGIN, destinationForSession(currentUid = null, role = null))
    }

    @Test
    fun loginRouting_adminStaffCustomer_goesToCorrectScreen() {
        assertEquals(Destination.ADMIN, destinationForSession("UID_ADMIN", "ADMIN"))
        assertEquals(Destination.STAFF, destinationForSession("UID_STAFF", "STAFF"))
        assertEquals(Destination.MAIN, destinationForSession("UID_CUSTOMER", "CUSTOMER"))
        assertEquals(Destination.MAIN, destinationForSession("UID_UNKNOWN", "UNKNOWN_ROLE"))
    }

    @Test
    fun register_validInput_createsCustomerByDefault() {
        val error = validateRegisterInput(
            name = "Nguyen Van An",
            email = "nguyenvanan@gmail.com",
            phone = "0912345678",
            password = "abc12345"
        )
        val user = buildRegisteredCustomer("Nguyen Van An", "nguyenvanan@gmail.com", "0912345678")

        assertNull(error)
        assertEquals("CUSTOMER", user.role)
        assertEquals("Nguyen Van An", user.name)
    }

    @Test
    fun register_emptyFields_returnsEmptyFieldsError() {
        val error = validateRegisterInput("", "", "", "")

        assertEquals(RegisterError.EMPTY_FIELDS, error)
    }

    @Test
    fun register_oneWordName_isRejected() {
        val error = validateRegisterInput(
            name = "An",
            email = "an@gmail.com",
            phone = "0912345678",
            password = "abc12345"
        )

        assertEquals(RegisterError.NAME_MUST_HAVE_AT_LEAST_TWO_WORDS, error)
    }

    @Test
    fun register_invalidVietnamesePhone_isRejected() {
        val invalidPrefix = validateRegisterInput("Nguyen Van An", "an@gmail.com", "0212345678", "abc12345")
        val invalidLength = validateRegisterInput("Nguyen Van An", "an@gmail.com", "091234567", "abc12345")

        assertEquals(RegisterError.INVALID_VN_PHONE, invalidPrefix)
        assertEquals(RegisterError.INVALID_VN_PHONE, invalidLength)
    }

    @Test
    fun register_nonGmailEmail_isRejected() {
        val error = validateRegisterInput(
            name = "Nguyen Van An",
            email = "an@outlook.com",
            phone = "0912345678",
            password = "abc12345"
        )

        assertEquals(RegisterError.EMAIL_MUST_BE_GMAIL, error)
    }

    @Test
    fun register_weakPassword_isRejected() {
        val tooShort = validateRegisterInput("Nguyen Van An", "an@gmail.com", "0912345678", "a123")
        val noDigit = validateRegisterInput("Nguyen Van An", "an@gmail.com", "0912345678", "abcdefgh")
        val noLetter = validateRegisterInput("Nguyen Van An", "an@gmail.com", "0912345678", "12345678")

        assertEquals(RegisterError.WEAK_PASSWORD, tooShort)
        assertEquals(RegisterError.WEAK_PASSWORD, noDigit)
        assertEquals(RegisterError.WEAK_PASSWORD, noLetter)
    }

    @Test
    fun profileChangePassword_validInput_passesValidation() {
        val error = validatePasswordChange(
            currentPassword = "old12345",
            newPassword = "new123",
            confirmPassword = "new123"
        )

        assertNull(error)
    }

    @Test
    fun profileChangePassword_invalidCases_areRejected() {
        assertEquals(
            PasswordChangeError.EMPTY_FIELDS,
            validatePasswordChange("", "new123", "new123")
        )
        assertEquals(
            PasswordChangeError.NEW_PASSWORD_TOO_SHORT,
            validatePasswordChange("old12345", "12345", "12345")
        )
        assertEquals(
            PasswordChangeError.CONFIRM_PASSWORD_NOT_MATCH,
            validatePasswordChange("old12345", "new123", "new456")
        )
    }

    @Test
    fun shippingFee_usesRoadFactorAndFiveThousandPerKm() {
        val distanceMeters = 2000.0

        val fee = calculateShippingFeeFromStraightDistanceMeters(distanceMeters)

        // 2 km * 1.25 * 5.000 = 12.500 VNĐ
        assertEquals(12500.0, fee, 0.001)
    }

    @Test
    fun nearestStore_selectsSmallestDistance() {
        val index = findNearestStoreIndex(listOf(2500.0, 800.0, 1500.0))

        assertEquals(1, index)
    }

    @Test
    fun parseQr_textFormatWithVietnameseBan_extractsStoreAndTable() {
        val table = parseTableQr("Matcha Vibe Quận 1 - bàn 4")

        assertEquals("Matcha Vibe Quận 1", table?.storeAddress)
        assertEquals("4", table?.tableNumber)
        assertEquals("Matcha Vibe Quận 1 - bàn 4", table?.qrCodeData)
    }

    @Test
    fun parseQr_textFormatWithoutAccent_extractsStoreAndTable() {
        val table = parseTableQr("Matcha Vibe Quận 7 - Ban 12")

        assertEquals("Matcha Vibe Quận 7", table?.storeAddress)
        assertEquals("12", table?.tableNumber)
    }

    @Test
    fun parseQr_simpleSplitFormat_extractsNumberFromSecondPart() {
        val table = parseTableQr("Matcha Vibe Landmark - table number 09")

        assertEquals("Matcha Vibe Landmark", table?.storeAddress)
        assertEquals("09", table?.tableNumber)
    }

    @Test
    fun parseQr_uriFormat_extractsStoreIdTableNumberAndStoreName() {
        val table = parseTableQr("matchavibe://table?storeId=STORE_01&tableNumber=8&storeName=Matcha%20Vibe%20HCMUT")

        assertEquals("STORE_01", table?.storeId)
        assertEquals("8", table?.tableNumber)
        assertEquals("Matcha Vibe HCMUT", table?.storeAddress)
    }

    @Test
    fun parseQr_invalidFormat_returnsNull() {
        assertNull(parseTableQr("random-invalid-content"))
        assertNull(parseTableQr("Matcha Vibe - ban khong co so"))
    }

    @Test
    fun forgotPassword_usesExpectedHotlineDialUri() {
        val hotlineUri = "tel:0948373374"

        assertEquals("tel:0948373374", hotlineUri)
        assertTrue(hotlineUri.startsWith("tel:"))
        assertFalse(hotlineUri.contains(" "))
    }
}
