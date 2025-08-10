package com.example.savehaven.data

/**
 * Our main Transaction model - represents every financial transaction
 * Works with Firestore for cloud storage
 */
data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(), // Timestamp in milliseconds
    val type: TransactionType = TransactionType.EXPENSE,
    val userId: String = "" // Links transaction to specific user
) {
    // Convert to Map format that Firestore can understand
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "amount" to amount,
            "category" to category,
            "description" to description,
            "date" to date,
            "type" to type.name, // Store enum as string
            "userId" to userId
        )
    }

    companion object {
        // Create Transaction from Firestore data - handles type conversion safely
        fun fromMap(map: Map<String, Any>, id: String): Transaction {
            return Transaction(
                id = id,
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0, // Safe number conversion
                category = map["category"] as? String ?: "",
                description = map["description"] as? String ?: "",
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                type = TransactionType.valueOf(map["type"] as? String ?: "EXPENSE"),
                userId = map["userId"] as? String ?: ""
            )
        }
    }
}

// Simple enum for transaction types
enum class TransactionType {
    INCOME, EXPENSE
}

// Predefined categories to keep things organized
object TransactionCategories {
    // Expense categories for young adults
    val EXPENSE_CATEGORIES = listOf(
        "Food",
        "Entertainment",
        "Rent",
        "Car",
        "Miscellaneous"
    )

    // Income sources for our target age group
    val INCOME_CATEGORIES = listOf(
        "Paycheck",
        "Gift",
        "Loan"
    )
}