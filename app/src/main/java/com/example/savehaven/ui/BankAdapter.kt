package com.example.savehaven.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.savehaven.R
import com.example.savehaven.data.Bank

class BankAdapter(
    private val banks: List<Bank>,
    private val onClick: (Bank) -> Unit
) : RecyclerView.Adapter<BankAdapter.BankViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BankViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bank, parent, false)
        return BankViewHolder(view)
    }

    override fun onBindViewHolder(holder: BankViewHolder, position: Int) {
        val bank = banks[position]
        holder.name.text = bank.name
        holder.address.text = bank.address
        holder.itemView.setOnClickListener { onClick(bank) }
    }

    override fun getItemCount() = banks.size

    class BankViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.bankName)
        val address: TextView = itemView.findViewById(R.id.bankAddress)
    }
}
