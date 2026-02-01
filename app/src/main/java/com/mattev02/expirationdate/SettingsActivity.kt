package com.mattev02.expirationdate

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.mattev02.expirationdate.settings.SettingsHelper

/**
 * Activity for application setting management.
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar : MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { view -> finish() }


        val earlyDayTextInput : TextInputEditText = findViewById(R.id.notification_days_input_text)
        earlyDayTextInput.setText(SettingsHelper.getEarlyNotificationDay(this).toString())
        earlyDayTextInput.doOnTextChanged { text, start, before, count ->
            try {
                val nDays = text.toString().toInt()
                if (nDays < 0) {
                    throw NumberFormatException("Days should be positive")
                }
                SettingsHelper.setEarlyNotificationDay(nDays, this)
            } catch (_ : NumberFormatException) {}
        }

        val earlyHourTextInput : TextInputEditText = findViewById(R.id.notification_hour_input_text)
        earlyHourTextInput.setText(SettingsHelper.getEarlyNotificationHour(this).toString())
        earlyHourTextInput.doOnTextChanged { text, start, before, count ->
            try {
                val nHour = text.toString().toInt()
                if (0 > nHour || nHour > 24) {
                    throw NumberFormatException("Hours should be in [0, 24]")
                }
                SettingsHelper.setEarlyNotificationHour(nHour, this)
            } catch (_ : NumberFormatException) {}
        }

        val defaultDaysExpirationTextInput : TextInputEditText = findViewById(R.id.default_expiration_date_input_text)
        defaultDaysExpirationTextInput.setText(SettingsHelper.getDefaultExpirationDay(this).toString())
        defaultDaysExpirationTextInput.doOnTextChanged { text, start, before, count ->
            try {
                val nDays = text.toString().toInt()
                if (nDays < 0) {
                    throw NumberFormatException("Days should be positive")
                }
                SettingsHelper.setDefaultExpirationDay(nDays, this)
            } catch (_ : NumberFormatException) {}
        }
    }
}