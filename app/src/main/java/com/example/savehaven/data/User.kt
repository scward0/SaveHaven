package com.example.savehaven.data

/**
 * User profile model - keeps track of user info and future gamification features
 * Firebase needs the empty constructor for deserialization
 */
data class User(
    val uid: String = "",          // Firebase Auth ID
    val username: String = "",     // Display name
    val email: String = "",        // Login email
    val level: Int = 1,           // For future gamification
    val points: Int = 0,          // For future gamification
    val createdAt: Long = System.currentTimeMillis() // When they joined
) {
    // Firebase requires this empty constructor for automatic object creation
    constructor() : this("", "", "", 1, 0, System.currentTimeMillis())
}