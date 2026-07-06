package com.example.matcha_vibe.model

data class Promo(
    var code: String = "",
    var discountPercent: Int = 0,
    var minOrderValue: Double = 0.0,
    var active: Boolean = true
)
