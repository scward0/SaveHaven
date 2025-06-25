package com.example.savehaven.data

data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val userId: String = ""
) {
    // Convert to map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "amount" to amount,
            "category" to category,
            "description" to description,
            "date" to date,
            "type" to type.name,
            "userId" to userId
        )
    }

    companion object {
        // Create from Firestore map
        fun fromMap(map: Map<String, Any>, id: String): Transaction {
            return Transaction(
                id = id,
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                category = map["category"] as? String ?: "",
                description = map["description"] as? String ?: "",
                date = (map["date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                type = TransactionType.valueOf(map["type"] as? String ?: "EXPENSE"),
                userId = map["userId"] as? String ?: ""
            )
        }
    }
}

enum class TransactionType {
    INCOME, EXPENSE
}

// Category constants
object TransactionCategories {
    val EXPENSE_CATEGORIES = listOf(
        "Food",
        "Entertainment",
        "Rent",
        "Car",
        "Miscellaneous"
    )

    val INCOME_CATEGORIES = listOf(
        "Paycheck",
        "Gift",
        "Loan"
    )
}