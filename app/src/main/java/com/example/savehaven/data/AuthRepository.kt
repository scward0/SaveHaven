package com.example.savehaven.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.savehaven.utils.Constants
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(username: String, email: String, password: String): Result<User> {
        return try {
            // Check if username is unique
            if (!isUsernameUnique(username)) {
                return Result.failure(Exception("Username already exists"))
            }

            // Create Firebase user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")

            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                username = username,
                email = email
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Login failed")

            // Get user data from Firestore
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

    suspend fun resetPassword(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isUsernameUnique(username: String): Boolean {
        return try {
            val querySnapshot = firestore.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .get()
                .await()

            querySnapshot.isEmpty
        } catch (e: Exception) {
            false // If error occurs, assume username is not unique to be safe
        }
    }

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return if (firebaseUser != null) {
            User(uid = firebaseUser.uid, email = firebaseUser.email ?: "")
        } else {
            null
        }
    }

    fun signOut() {
        auth.signOut()
    }
}