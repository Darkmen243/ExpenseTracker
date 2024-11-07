package com.example.expensetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class LimitNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
            sendNotification(context)
    }

    private fun sendNotification(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationCompat.Builder(context, "limit_channel")
                .setSmallIcon(R.drawable.ic_limit_warning)
                .setContentTitle("Monthly Limit Warning")
                .setContentText("You have reached 80% of your monthly spending limit.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(1001, notification)
        } else {
            Log.w("LimitNotificationReceiver", "Notification permission not granted")
        }
    }
}
