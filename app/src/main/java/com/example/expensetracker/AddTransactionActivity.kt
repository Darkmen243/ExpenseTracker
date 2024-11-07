package com.example.expensetracker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var inputTitle: EditText
    private lateinit var inputAmount: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var btnSaveTransaction: Button
    private lateinit var textDate: TextView
    private var editingTransaction: Transaction? = null
    private var selectedDate: Long = System.currentTimeMillis()

    private val categories = listOf("Food", "Transport", "Entertainment", "Utilities", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        inputTitle = findViewById(R.id.inputTitle)
        inputAmount = findViewById(R.id.inputAmount)
        categorySpinner = findViewById(R.id.categorySpinner)
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction)
        textDate = findViewById(R.id.textDate)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        editingTransaction = intent.getParcelableExtra("editTransaction")
        if (editingTransaction != null) {
            inputTitle.setText(editingTransaction!!.title)
            inputAmount.setText(editingTransaction!!.amount.toString())
            textDate.text = editingTransaction!!.date.toString()
            selectedDate = editingTransaction!!.date.time
            val spinnerPosition = categories.indexOf(editingTransaction!!.category)
            if (spinnerPosition >= 0) {
                categorySpinner.setSelection(spinnerPosition)
            }
        }

        textDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Transaction Date")
                .setSelection(selectedDate)
                .build()

            datePicker.addOnPositiveButtonClickListener {
                selectedDate = it
                textDate.text = Date(it).toString()
            }
            datePicker.show(supportFragmentManager, "DATE_PICKER")
        }

        btnSaveTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    private fun saveTransaction() {
        val title = inputTitle.text.toString()
        val amountText = inputAmount.text.toString()
        val category = categorySpinner.selectedItem?.toString()

        if (title.isNotEmpty() && amountText.isNotEmpty()) {
            val amount = amountText.toDoubleOrNull()
            if (amount != null) {
                val values = ContentValues().apply {
                    put("title", title)
                    put("amount", amount)
                    put("category", category ?: "General")
                    put("date", selectedDate)
                }

                val uri = if (editingTransaction == null) {
                    contentResolver.insert(TransactionContentProvider.CONTENT_URI, values)
                } else {
                    val editUri = Uri.withAppendedPath(
                        TransactionContentProvider.CONTENT_URI,
                        editingTransaction!!.id.toString()
                    )
                    contentResolver.update(editUri, values, null, null)
                    editUri
                }

                if (uri != null) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("transactionUri", uri)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
    }
}
