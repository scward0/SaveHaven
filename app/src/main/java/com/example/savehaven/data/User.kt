package com.example.savehaven.data

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val level: Int = 1,
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", 1, 0, System.currentTimeMillis())
}