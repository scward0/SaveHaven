package com.example.savehaven.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Repository for all transaction database operations
 * Handles CRUD operations with Firestore and keeps user data separate
 */
class TransactionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "TransactionRepository"
    }

    // Helper to get current user ID for data isolation
    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    // Add new transaction to database
    fun addTransaction(transaction: Transaction, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        // Create transaction with user ID and generate unique ID
        val transactionWithUser = transaction.copy(
            userId = userId,
            id = db.collection("transactions").document().id
        )

        // Save to Firestore
        db.collection("transactions")
            .document(transactionWithUser.id)
            .set(transactionWithUser.toMap())
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }

    // Get all transactions for current user
    fun getUserTransactions(callback: (List<Transaction>, String?) -> Unit) {
        val userId = getCurrentUserId()

        if (userId == null) {
            callback(emptyList(), "User not authenticated")
            return
        }

        // Query only this user's transactions
        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->

                // Convert documents to Transaction objects, skip any that are malformed
                val transactions = documents.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "Processing document: ${doc.id}")
                        Transaction.fromMap(doc.data, doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction document ${doc.id}", e)
                        null // Skip bad documents instead of crashing
                    }
                }

                // Sort by date (newest first) for better UX
                val sortedTransactions = transactions.sortedByDescending { it.date }

                callback(sortedTransactions, null)
            }
            .addOnFailureListener { exception ->
                callback(emptyList(), exception.message)
            }
    }

    // Update existing transaction
    fun updateTransaction(transaction: Transaction, callback: (Boolean, String?) -> Unit) {
        if (transaction.id.isEmpty()) {
            callback(false, "Invalid transaction ID")
            return
        }

        // Update the document in Firestore
        db.collection("transactions")
            .document(transaction.id)
            .update(transaction.toMap())
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }

    // Delete transaction
    fun deleteTransaction(transactionId: String, callback: (Boolean, String?) -> Unit) {
        if (transactionId.isEmpty()) {
            callback(false, "Invalid transaction ID")
            return
        }

        // Remove from Firestore
        db.collection("transactions")
            .document(transactionId)
            .delete()
            .addOnSuccessListener {
                callback(true, null)
            }
            .addOnFailureListener { exception ->
                callback(false, exception.message)
            }
    }
}