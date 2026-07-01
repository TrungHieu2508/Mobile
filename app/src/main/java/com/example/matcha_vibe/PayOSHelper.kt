package com.example.matcha_vibe

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PayOSHelper {
    private const val CLIENT_ID = "f062487f-6f2c-49be-944d-de17e3a3bc12"
    private const val API_KEY = "fe5103dc-2342-4258-bdf3-324aa07f5886"
    private const val CHECKSUM_KEY = "7c0c74fd058f1b3ddab14050a06f973c64a544ee422dcb34f95d4531a397cae0"
    
    private val client = OkHttpClient()
    private val gson = Gson()

    fun createPaymentLink(
        amount: Int,
        description: String,
        onSuccess: (String, Long) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val orderCode = System.currentTimeMillis() / 1000 // Sử dụng timestamp làm mã đơn hàng

        // 1. Tạo dữ liệu thanh toán
        val bodyMap = mutableMapOf<String, Any>()
        bodyMap["orderCode"] = orderCode
        bodyMap["amount"] = amount
        bodyMap["description"] = description
        bodyMap["cancelUrl"] = "matchavibe://cancel"
        bodyMap["returnUrl"] = "matchavibe://success"

        // 2. Tạo chữ ký (Signature)
        val signature = createSignature(bodyMap)
        bodyMap["signature"] = signature

        val jsonBody = gson.toJson(bodyMap)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api-merchant.payos.vn/v2/payment-requests")
            .addHeader("x-client-id", CLIENT_ID)
            .addHeader("x-api-key", API_KEY)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    val jsonResponse = gson.fromJson(responseData, JsonObject::class.java)
                    if (jsonResponse.get("code").asString == "00") {
                        val data = jsonResponse.getAsJsonObject("data")
                        val checkoutUrl = data.get("checkoutUrl").asString
                        onSuccess(checkoutUrl, orderCode)
                    } else {
                        onFailure(jsonResponse.get("desc").asString)
                    }
                } else {
                    onFailure("Error: ${response.code}")
                }
            }
        })
    }

    private fun createSignature(data: Map<String, Any>): String {
        val sortedData = data.toSortedMap()
        val signData = sortedData.entries.joinToString("&") { "${it.key}=${it.value}" }
        return hmacSha256(CHECKSUM_KEY, signData)
    }

    private fun hmacSha256(key: String, data: String): String {
        val sha256Hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val bytes = sha256Hmac.doFinal(data.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
