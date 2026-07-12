package com.example.matcha_vibe.model

data class User(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var phone: String = "",
    var role: String = "CUSTOMER" // "ADMIN", "STAFF", "CUSTOMER"
)
