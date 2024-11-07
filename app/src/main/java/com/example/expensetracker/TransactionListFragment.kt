package com.example.expensetracker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class TransactionListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var btnAddTransaction: ExtendedFloatingActionButton
    private lateinit var spinnerCategoryFilter: Spinner
    private lateinit var btnShowStatistics: Button
    private lateinit var btnSaveLimit : Button
    private val transactions = mutableListOf<Transaction>()
    private val filteredTransactions = mutableListOf<Transaction>()
    private val categories = listOf("All", "Food", "Transport", "Entertainment", "Utilities", "Other")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        btnAddTransaction = view.findViewById(R.id.fabAddTransaction)
        btnShowStatistics = view.findViewById(R.id.btnShowStatistics)
        spinnerCategoryFilter = view.findViewById(R.id.spinnerCategoryFilter)

        val editTextMonthlyLimit = view.findViewById<EditText>(R.id.editTextMonthlyLimit)
        btnSaveLimit = view.findViewById(R.id.buttonSaveLimit)
        setupRecyclerView()
        setupCategoryFilter()

        btnAddTransaction.setOnClickListener {
            addTransaction()
        }
        btnShowStatistics.setOnClickListener {
            navigateToStatisticsFragment()
        }
        btnSaveLimit.setOnClickListener {
            val limit = editTextMonthlyLimit.text.toString().toDoubleOrNull()
            if (limit != null) {
                setupMonthlyLimitSetting()
            }
        }
        loadTransactions()
        return view
    }
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            filteredTransactions,
            onEditClick = { transaction -> editTransaction(transaction) },
            onDeleteClick = { transaction -> deleteTransaction(transaction) }
        )
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = transactionAdapter
    }
    private fun setupCategoryFilter() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoryFilter.adapter = adapter

        spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                filterTransactionsByCategory(categories[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    private fun addTransaction() {
        val intent = Intent(context, AddTransactionActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_ADD)
    }
    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(context, AddTransactionActivity::class.java).apply {
            putExtra("editTransaction", transaction)
        }
        startActivityForResult(intent, REQUEST_CODE_EDIT)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            loadTransactions()
            checkMonthlyLimit()
        }
    }
    private fun loadTransactions() {
        transactions.clear()
        val cursor = context?.contentResolver?.query(
            TransactionContentProvider.CONTENT_URI, null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val idIndex = it.getColumnIndex("id")
                val titleIndex = it.getColumnIndex("title")
                val amountIndex = it.getColumnIndex("amount")
                val categoryIndex = it.getColumnIndex("category")
                val dateIndex = it.getColumnIndex("date")
                if (idIndex != -1 && titleIndex != -1 && amountIndex != -1 && categoryIndex != -1 && dateIndex != -1) {
                    val id = it.getLong(idIndex)
                    val title = it.getString(titleIndex)
                    val amount = it.getDouble(amountIndex)
                    val category = it.getString(categoryIndex)
                    val dateMillis = it.getLong(dateIndex)
                    val date = Date(dateMillis)

                    transactions.add(Transaction(id, title, amount, category, date))
                } else {
                    Log.e("TransactionListFragment", "Invalid column index")
                }
            }
        }
        filterTransactionsByCategory(spinnerCategoryFilter.selectedItem.toString())
    }

    private fun filterTransactionsByCategory(category: String) {
        filteredTransactions.clear()
        if (category == "All") {
            filteredTransactions.addAll(transactions)
        } else {
            filteredTransactions.addAll(transactions.filter { it.category == category })
        }
        transactionAdapter.notifyDataSetChanged()
    }

    private fun deleteTransaction(transaction: Transaction) {
        val uri = Uri.withAppendedPath(TransactionContentProvider.CONTENT_URI, transaction.id.toString())
        context?.contentResolver?.delete(uri, null, null)
        transactions.remove(transaction)
        filterTransactionsByCategory(spinnerCategoryFilter.selectedItem.toString())
        Snackbar.make(requireView(), "Transaction deleted", Snackbar.LENGTH_SHORT).show()
    }
    private fun navigateToStatisticsFragment() {
        val statisticsFragment = StatisticsFragment()
        val bundle = Bundle()
        bundle.putParcelableArrayList("transactions", ArrayList(filteredTransactions))
        statisticsFragment.arguments = bundle
        requireActivity().supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_host_fragment, statisticsFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun setupMonthlyLimitSetting() {
        val editTextMonthlyLimit = view?.findViewById<EditText>(R.id.editTextMonthlyLimit)

        view?.findViewById<Button>(R.id.buttonSaveLimit)?.setOnClickListener {
            val limit = editTextMonthlyLimit?.text.toString().toDoubleOrNull()
            if (limit != null) {
                val sharedPref = requireContext().getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
                sharedPref.edit().putString("monthly_limit", limit.toString()).apply()
                Toast.makeText(context, "Monthly limit saved.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Please enter a valid limit.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun checkMonthlyLimit() {
        val sharedPref = requireContext().getSharedPreferences("ExpensePrefs", Context.MODE_PRIVATE)
        val limit = sharedPref.getString("monthly_limit", "0.0")?.toDouble() ?: 0.0

        if (limit > 0) {
            val currentMonthExpenses = transactions.filter {
                val calendar = Calendar.getInstance()
                calendar.time = it.date
                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)
                month == Calendar.getInstance().get(Calendar.MONTH) && year == Calendar.getInstance().get(Calendar.YEAR)
            }.sumOf { it.amount }

            if (currentMonthExpenses >= 0.8 * limit) {
                val intent = Intent(context, LimitNotificationReceiver::class.java).apply {
                    putExtra("monthlyLimit", limit)
                    putExtra("currentSpending", currentMonthExpenses)
                }
                context?.sendBroadcast(intent)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        view?.findViewById<EditText>(R.id.editTextMonthlyLimit)?.clearFocus()
    }

    companion object {
        private const val REQUEST_CODE_ADD = 1
        private const val REQUEST_CODE_EDIT = 2
    }
}
