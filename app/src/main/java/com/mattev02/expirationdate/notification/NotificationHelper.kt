package com.mattev02.expirationdate.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mattev02.expirationdate.itemlist.Item
import com.mattev02.expirationdate.settings.SettingsHelper
import java.time.ZoneId

object NotificationHelper {

    private const val CHANNEL_ID = "EXPIRY_CHANNEL_ID"
    private const val CHANNEL_NAME = "Expiration Alerts"
    private const val CHANNEL_DESC = "Notifications for expiring items"

    fun createNotificationChannel(context: Context) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESC
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleNotification(context: Context, item: Item) {
        if (item.expirationDate == null || !item.taken) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ExpiryNotificationReceiver::class.java).apply {
            putExtra("ITEM_NAME", item.name)
            putExtra("ITEM_ID", item.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id, // Unique ID per item
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val date = item.expirationDate!!
        val triggerDateTime = date.minusDays(SettingsHelper.getEarlyNotificationDay(context).toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val hourOffset = SettingsHelper.getEarlyNotificationHour(context) * 60 * 60 * 1000
        val finalTriggerTime = triggerDateTime + hourOffset

        if (finalTriggerTime > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    // Fallback for Android 12+ if permission is missing
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                } else {
                    // Exact alarm
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalTriggerTime, pendingIntent)
                }
                Log.d("NotificationHelper", "Scheduled for ${item.name} at $finalTriggerTime")
            } catch (e: SecurityException) {
                Log.e("NotificationHelper", "Permission error: ${e.message}")
            }
        }
    }

    fun cancelNotification(context: Context, item: Item) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ExpiryNotificationReceiver::class.java)

        // Must match the PendingIntent structure exactly to cancel it
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)
        Log.d("NotificationHelper", "Cancelled for ${item.name}")
    }
}