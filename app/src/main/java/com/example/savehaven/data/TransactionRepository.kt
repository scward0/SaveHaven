package com.example.savehaven.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TransactionRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "TransactionRepository"
    }

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun addTransaction(transaction: Transaction, callback: (Boolean, String?) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }

        val transactionWithUser = transaction.copy(
            userId = userId,
            id = db.collection("transactions").document().id
        )

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

    fun getUserTransactions(callback: (List<Transaction>, String?) -> Unit) {
        val userId = getCurrentUserId()
        Log.d(TAG, "getUserTransactions called for userId: $userId")

        if (userId == null) {
            Log.w(TAG, "User not authenticated")
            callback(emptyList(), "User not authenticated")
            return
        }

        Log.d(TAG, "Making Firestore query for userId: $userId")


        db.collection("transactions")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Firestore query successful. Document count: ${documents.size()}")

                val transactions = documents.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "Processing document: ${doc.id}")
                        Transaction.fromMap(doc.data, doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction document ${doc.id}", e)
                        null // Skip malformed documents
                    }
                }

                // Sort by date in descending order (most recent first)
                val sortedTransactions = transactions.sortedByDescending { it.date }

                Log.d(TAG, "Successfully parsed ${sortedTransactions.size} transactions")
                callback(sortedTransactions, null)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Firestore query failed", exception)
                callback(emptyList(), exception.message)
            }
    }

    fun updateTransaction(transaction: Transaction, callback: (Boolean, String?) -> Unit) {
        if (transaction.id.isEmpty()) {
            callback(false, "Invalid transaction ID")
            return
        }

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

    fun deleteTransaction(transactionId: String, callback: (Boolean, String?) -> Unit) {
        if (transactionId.isEmpty()) {
            callback(false, "Invalid transaction ID")
            return
        }

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

