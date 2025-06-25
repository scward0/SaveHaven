package com.example.savehaven.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.savehaven.R
import com.example.savehaven.data.Transaction
import com.example.savehaven.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardTransactionAdapter(
    private val onTransactionClick: (Transaction) -> Unit
) : RecyclerView.Adapter<DashboardTransactionAdapter.TransactionViewHolder>() {

    private var transactions = listOf<Transaction>()
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.US)
    private val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun updateTransactions(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(transaction: Transaction) {
            tvCategory.text = transaction.category
            tvDate.text = dateFormat.format(Date(transaction.date))
            tvDescription.text = if (transaction.description.isNotEmpty()) {
                transaction.description
            } else {
                "No description"
            }

            // Format amount with + or - prefix and appropriate color
            val amountText = when (transaction.type) {
                TransactionType.INCOME -> "+${numberFormat.format(transaction.amount)}"
                TransactionType.EXPENSE -> "-${numberFormat.format(transaction.amount)}"
            }
            tvAmount.text = amountText

            // Set color based on transaction type
            val color = when (transaction.type) {
                TransactionType.INCOME -> ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark)
                TransactionType.EXPENSE -> ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark)
            }
            tvAmount.setTextColor(color)

            // Set click listener
            itemView.setOnClickListener {
                onTransactionClick(transaction)
            }
        }
    }
}