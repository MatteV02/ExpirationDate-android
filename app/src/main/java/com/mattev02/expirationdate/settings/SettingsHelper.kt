package com.mattev02.expirationdate.settings

import android.content.Context
import androidx.core.content.edit

object SettingsHelper {
    const val SETTINGS_FILE = "appSettings"
    const val EARLY_NOTIFICATION_DAY = "early_notification_day"
    const val EARLY_NOTIFICATION_HOUR = "early_notification_hour"
    const val DEFAULT_EXPIRATION_DATE = "default_expiration_date"

    fun getEarlyNotificationDay(context: Context) : Int {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(EARLY_NOTIFICATION_DAY, 7)
    }

    fun setEarlyNotificationDay(nDays: Int, context: Context) {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putInt(EARLY_NOTIFICATION_DAY, nDays)
        }
    }

    fun getEarlyNotificationHour(context: Context) : Int {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(EARLY_NOTIFICATION_HOUR, 9)
    }

    fun setEarlyNotificationHour(nHour: Int, context: Context) {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putInt(EARLY_NOTIFICATION_HOUR, nHour)
        }
    }

    fun getDefaultExpirationDay(context: Context) : Int {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(DEFAULT_EXPIRATION_DATE, 9)
    }

    fun setDefaultExpirationDay(nDays: Int, context: Context) {
        val sharedPreferences = context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putInt(DEFAULT_EXPIRATION_DATE, nDays)
        }
    }
}