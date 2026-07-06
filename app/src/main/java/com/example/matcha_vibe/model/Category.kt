package com.example.matcha_vibe.model

data class Category(
    var id: String = "",
    var name: String = ""
) {
    override fun toString(): String {
        return name
    }
}
