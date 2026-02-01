package com.mattev02.expirationdate.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mattev02.expirationdate.ItemDetailActivity
import com.mattev02.expirationdate.R

class ExpiryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemName = intent.getStringExtra("ITEM_NAME") ?: "Item"
        val itemId = intent.getIntExtra("ITEM_ID", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create an intent to open the app when clicking the notification
        val activityIntent = Intent(context, ItemDetailActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("itemId", itemId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, "EXPIRY_CHANNEL_ID")
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app icon
            .setContentTitle("Expiration Warning")
            .setContentText("$itemName ${context.getString(R.string.expiration_notification)}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(itemId, notification)
    }
}