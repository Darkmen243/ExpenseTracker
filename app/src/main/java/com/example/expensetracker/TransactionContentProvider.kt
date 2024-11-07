package com.example.expensetracker

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import java.util.Date

class TransactionContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.expensetracker.provider"
        const val TRANSACTIONS = 1
        const val TRANSACTIONS_ID = 2
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/transactions")
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "transactions", TRANSACTIONS)
            addURI(AUTHORITY, "transactions/#", TRANSACTIONS_ID)
        }
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            TRANSACTIONS -> {
                val cursor = MatrixCursor(arrayOf("id", "title", "amount", "category", "date"))
                for (transaction in TransactionRepository.transactions) {
                    cursor.addRow(arrayOf(transaction.id, transaction.title, transaction.amount, transaction.category, transaction.date.time))
                }
                cursor
            }
            TRANSACTIONS_ID -> {
                val id = uri.lastPathSegment?.toLong() ?: return null
                val transaction = TransactionRepository.transactions.find { it.id == id }
                if (transaction != null) {
                    val cursor = MatrixCursor(arrayOf("id", "title", "amount", "category", "date"))
                    cursor.addRow(arrayOf(transaction.id, transaction.title, transaction.amount, transaction.category, transaction.date.time))
                    cursor
                } else null
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            TRANSACTIONS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.transactions"
            TRANSACTIONS_ID -> "vnd.android.cursor.item/vnd.$AUTHORITY.transactions"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val title = values?.getAsString("title") ?: return null
        val amount = values.getAsDouble("amount") ?: return null
        val category = values.getAsString("category") ?: "General"
        val dateMillis = values.getAsLong("date") ?: Date().time

        val newId = (TransactionRepository.transactions.maxOfOrNull { it.id } ?: 0L) + 1
        val newTransaction = Transaction(newId, title, amount, category, Date(dateMillis))
        TransactionRepository.transactions.add(newTransaction)

        return Uri.withAppendedPath(CONTENT_URI, newId.toString())
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        val id = uri.lastPathSegment?.toLong() ?: return 0
        val transaction = TransactionRepository.transactions.find { it.id == id }
        if (transaction != null && values != null) {
            transaction.title = values.getAsString("title") ?: transaction.title
            transaction.amount = values.getAsDouble("amount") ?: transaction.amount
            transaction.category = values.getAsString("category") ?: transaction.category
            val dateMillis = values.getAsLong("date") ?: transaction.date.time
            transaction.date = Date(dateMillis)
            return 1
        }
        return 0
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return when (uriMatcher.match(uri)) {
            TRANSACTIONS_ID -> {
                val id = uri.lastPathSegment?.toLong() ?: return 0
                val removed = TransactionRepository.transactions.removeIf { it.id == id }
                if (removed) 1 else 0
            }
            else -> 0
        }
    }
}
