package com.example.savehaven.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.savehaven.R
import com.example.savehaven.data.Transaction
import com.example.savehaven.data.TransactionType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for the transaction history screen with filtering
 * More detailed layout than dashboard adapter
 */
class TransactionHistoryAdapter(
    private var transactions: List<Transaction>,
    private val onTransactionClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
        val tvDescription: TextView = itemView.findViewById(R.id.tvTransactionDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_history, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Basic transaction info
        holder.tvCategory.text = transaction.category
        holder.tvDescription.text = if (transaction.description.isNotEmpty()) {
            transaction.description
        } else {
            "No description"
        }

        // Full date format for history view
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(transaction.date))

        // Amount formatting
        val formattedAmount = String.format("%.2f", transaction.amount)
        holder.tvAmount.text = "$$formattedAmount"

        // Transaction type with color coding
        if (transaction.type == TransactionType.INCOME) {
            holder.tvType.text = "INCOME"
            holder.tvType.setTextColor(0xFF4CAF50.toInt()) // Green
            holder.tvAmount.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.tvType.text = "EXPENSE"
            holder.tvType.setTextColor(0xFFF44336.toInt()) // Red
            holder.tvAmount.setTextColor(0xFFF44336.toInt())
        }

        // Click to edit
        holder.itemView.setOnClickListener {
            onTransactionClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    // Update transaction list (for filtering)
    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}