package com.example.savehaven.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.savehaven.utils.Constants
import kotlinx.coroutines.tasks.await

/**
 * Handles all the authentication stuff - registration, login, password reset
 * Uses Firebase Auth + Firestore to store user profiles
 */
class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Register new user - checks username uniqueness first
    suspend fun registerUser(username: String, email: String, password: String): Result<User> {
        return try {
            // Make sure username isn't already taken
            if (!isUsernameUnique(username)) {
                return Result.failure(Exception("Username already exists"))
            }

            // Create the Firebase auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            // Create user profile in our Firestore database
            val user = User(
                uid = firebaseUser.uid,
                username = username,
                email = email
            )

            // Save user profile to database
            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login existing user
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            // Authenticate with Firebase
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")

            // Get their profile data from Firestore
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("User data not found")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Send password reset email
    suspend fun resetPassword(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if username is available
    suspend fun isUsernameUnique(username: String): Boolean {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .get()
                .await()

            querySnapshot.isEmpty // True if no matches found
        } catch (e: Exception) {
            false // Assume not unique if we can't check
        }
    }

    // Get current user info (basic)
    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
        } else {
            null
        }
    }

    // Sign out current user
    fun signOut() {
        auth.signOut()
    }
}