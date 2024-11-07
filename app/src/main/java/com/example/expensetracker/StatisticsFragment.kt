package com.example.expensetracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.util.*
import java.util.concurrent.TimeUnit

class StatisticsFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var backButton: Button
    private lateinit var timePeriodSpinner: Spinner
    private var selectedPeriod: String = "Day"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        pieChart = view.findViewById(R.id.pieChart)
        backButton = view.findViewById(R.id.btnShowTransactionList)
        timePeriodSpinner = view.findViewById(R.id.spinnerTimePeriod)

        setupTimePeriodSpinner()
        setupPieChart()

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        return view
    }

    private fun setupTimePeriodSpinner() {
        val timePeriods = listOf("Day", "Week", "Month")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timePeriods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timePeriodSpinner.adapter = adapter

        timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedPeriod = timePeriods[position]
                setupPieChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupPieChart() {
        val filteredTransactions = filterTransactionsByPeriod()
        val categoryAmounts = mutableMapOf<String, Double>()

        for (transaction in filteredTransactions) {
            categoryAmounts[transaction.category] = categoryAmounts.getOrDefault(transaction.category, 0.0) + transaction.amount
        }

        val pieEntries = categoryAmounts.map { PieEntry(it.value.toFloat(), it.key) }

        if (pieEntries.isNotEmpty()) {
            val pieDataSet = PieDataSet(pieEntries, "Expenses by Category")
            pieDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
            val pieData = PieData(pieDataSet)
            pieChart.data = pieData
            pieChart.invalidate()
        } else {
            pieChart.clear()
        }
    }

    private fun filterTransactionsByPeriod(): List<Transaction> {
        val transactionList = mutableListOf<Transaction>()
        val calendar = Calendar.getInstance()
        val currentDate = Date()

        val cursor = requireContext().contentResolver.query(
            TransactionContentProvider.CONTENT_URI,
            null, null, null, null
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

                    transactionList.add(Transaction(id, title, amount, category, date))
                }
            }
        }

        return when (selectedPeriod) {
            "Day" -> transactionList.filter {
                val daysDiff = TimeUnit.DAYS.convert(currentDate.time - it.date.time, TimeUnit.MILLISECONDS)
                daysDiff == 0L
            }
            "Week" -> transactionList.filter {
                calendar.time = currentDate
                val currentWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                calendar.time = it.date
                val transactionWeek = calendar.get(Calendar.WEEK_OF_YEAR)
                currentWeek == transactionWeek && calendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }
            "Month" -> transactionList.filter {
                calendar.time = currentDate
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                calendar.time = it.date
                val transactionMonth = calendar.get(Calendar.MONTH)
                val transactionYear = calendar.get(Calendar.YEAR)
                currentMonth == transactionMonth && currentYear == transactionYear
            }
            else -> transactionList
        }
    }
}
