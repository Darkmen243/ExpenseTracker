package com.example.expensetracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val transactions: MutableList<Transaction>,
    private val onEditClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)

        holder.editButton.setOnClickListener {
           onEditClick(transaction)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(transaction)
        }
    }

    override fun getItemCount(): Int = transactions.size

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textTitle)
        private val amountTextView: TextView = itemView.findViewById(R.id.textAmount)
        private val categoryTextView: TextView = itemView.findViewById(R.id.textCategory)
        val editButton: Button = itemView.findViewById(R.id.btnEditransaction)
        val deleteButton: Button = itemView.findViewById(R.id.btnDeleteTransaction)
        val dateTextView: TextView = itemView.findViewById(R.id.textDate)
        fun bind(transaction: Transaction) {
            titleTextView.text = transaction.title
            amountTextView.text = transaction.amount.toString()
            categoryTextView.text = transaction.category
            dateTextView.text = transaction.date.toString()
        }
    }
}
