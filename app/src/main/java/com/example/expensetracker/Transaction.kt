package com.example.expensetracker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Transaction(
    val id: Long,
    var title: String,
    var amount: Double,
    var category: String,
    var date: Date,
) : Parcelable
