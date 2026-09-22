package com.example.thornburydental.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.thornburydental.data.Appointment
import com.example.thornburydental.data.db.LocalDatabaseManager
import com.example.thornburydental.util.parseTimeToMinutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Manages notification channels and exact/inexact AlarmManager alarms
 * for:
 * 1. Daily morning briefing: listing all patients booked today at clinician's set time.
 * 2. Chairside patient arrival: alerting N minutes before a patient's appointment.
 */
object ReminderManager {

    private const val TAG = "ReminderManager"

    const val CHANNEL_DAILY_BRIEFING = "practix_daily_briefing"
    const val CHANNEL_PATIENT_ARRIVAL = "practix_patient_arrival"

    const val ACTION_DAILY_MORNING_BRIEFING = "com.practix.dental.ACTION_DAILY_MORNING_BRIEFING"
    const val ACTION_PATIENT_ARRIVAL = "com.practix.dental.ACTION_PATIENT_ARRIVAL"

    const val EXTRA_APPT_ID = "extra_appt_id"
    const val EXTRA_PATIENT_NAME = "extra_patient_name"
    const val EXTRA_PROCEDURE = "extra_procedure"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_LEAD_MIN = "extra_lead_min"

    private const val REQUEST_CODE_DAILY_BRIEFING = 7001

    /**
     * Create notification channels on Android 8.0+ (Oreo).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Channel 1: Daily Morning Schedule Briefing
            val briefingChannel = NotificationChannel(
                CHANNEL_DAILY_BRIEFING,
                "Daily Morning Schedule Briefing",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you every morning of today's booked patients and clinical schedule."
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel 2: Patient Arrival Alert
            val arrivalChannel = NotificationChannel(
                CHANNEL_PATIENT_ARRIVAL,
                "Patient Chairside Arrival Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Chairside alerts before a patient's scheduled appointment time."
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(briefingChannel)
            notificationManager.createNotificationChannel(arrivalChannel)
            Log.d(TAG, "Notification channels initialized.")
        }
    }

    /**
     * Schedule daily morning briefing alarm at the specified time string (e.g. "08:00" or "8:00 AM").
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyMorningBriefing(context: Context, timeString: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DentaraAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_BRIEFING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (hour, minute) = parseHourMinute(timeString)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow morning
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Daily morning briefing alarm scheduled for: ${calendar.time}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing; scheduling inexact alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel the daily morning briefing alarm.
     */
    fun cancelDailyMorningBriefing(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DentaraAlarmReceiver::class.java).apply {
            action = ACTION_DAILY_MORNING_BRIEFING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_BRIEFING,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Daily morning briefing alarm cancelled.")
        }
    }

    /**
     * Schedule a chairside arrival reminder for a specific appointment.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun schedulePatientArrivalReminder(context: Context, appointment: Appointment) {
        if (!appointment.reminderEnabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerMillis = computeAppointmentReminderTime(
            dateIso = appointment.date,
            timeStr = appointment.time,
            leadMinutes = appointment.reminderLeadTimeMin
        )

        // If the appointment or alert window has already passed, don't schedule
        if (triggerMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Reminder trigger time has already passed for appointment ${appointment.id}")
            return
        }

        val requestCode = appointment.id.hashCode()
        val intent = Intent(context, DentaraAlarmReceiver::class.java).apply {
            action = ACTION_PATIENT_ARRIVAL
            putExtra(EXTRA_APPT_ID, appointment.id)
            putExtra(EXTRA_PATIENT_NAME, appointment.patientName)
            putExtra(EXTRA_PROCEDURE, appointment.procedure)
            putExtra(EXTRA_TIME, appointment.time)
            putExtra(EXTRA_LEAD_MIN, appointment.reminderLeadTimeMin)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Patient arrival alarm scheduled for ${appointment.patientName} at epoch $triggerMillis (${appointment.reminderLeadTimeMin}m prior)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission missing; scheduling inexact alarm", e)
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel an existing patient arrival reminder.
     */
    fun cancelPatientArrivalReminder(context: Context, appointmentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = appointmentId.hashCode()
        val intent = Intent(context, DentaraAlarmReceiver::class.java).apply {
            action = ACTION_PATIENT_ARRIVAL
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Patient arrival alarm cancelled for appointment $appointmentId")
        }
    }

    /**
     * Reschedule all active reminders (e.g. after reboot or application initialization).
     */
    fun rescheduleAllActiveReminders(context: Context) {
        createNotificationChannels(context)

        if (!LocalDatabaseManager.isInitialized) return

        try {
            // 1. Reschedule Morning Briefing if enabled
            val prefs = LocalDatabaseManager.userPreferencesDao.getPreferences()
            if (prefs != null && prefs.morningReminderEnabled) {
                scheduleDailyMorningBriefing(context, prefs.morningReminderTime)
            }

            // 2. Reschedule Chairside Patient Reminders
            val activeAppointments = LocalDatabaseManager.appointmentDao.getAppointmentsWithActiveReminders()
            activeAppointments.forEach { appt ->
                schedulePatientArrivalReminder(context, appt)
            }
            Log.d(TAG, "Rescheduled ${activeAppointments.size} active chairside patient reminders.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reschedule active reminders", e)
        }
    }

    /**
     * Compute trigger epoch millis by parsing ISO date "yyyy-MM-dd" and time string "hh:mm AM/PM"
     * minus leadMinutes.
     */
    fun computeAppointmentReminderTime(dateIso: String, timeStr: String, leadMinutes: Int): Long {
        val cal = Calendar.getInstance()

        // 1. Parse date
        if (dateIso.isNotBlank()) {
            val dateParts = dateIso.trim().split("-")
            if (dateParts.size == 3) {
                val year = dateParts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
                val month = (dateParts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                val day = dateParts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                cal.set(year, month, day)
            }
        }

        // 2. Parse time (e.g. "09:30 AM" or "14:00")
        val minutesFromMidnight = parseTimeToMinutes(timeStr)
        val hour = minutesFromMidnight / 60
        val minute = minutesFromMidnight % 60

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // 3. Subtract lead time
        cal.add(Calendar.MINUTE, -leadMinutes)

        return cal.timeInMillis
    }

    fun formatTime12Hour(timeString: String): String {
        val trimmed = timeString.trim()
        if (trimmed.isBlank()) return timeString

        val is12Hour = trimmed.contains("am", ignoreCase = true) || trimmed.contains("pm", ignoreCase = true)
        if (is12Hour) {
            return try {
                val sdf = SimpleDateFormat("h:mm a", Locale.US)
                val date = sdf.parse(trimmed)
                if (date != null) sdf.format(date) else timeString
            } catch (_: Exception) {
                timeString
            }
        }

        val parts = trimmed.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull()
            val minute = parts[1].toIntOrNull()
            if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                return SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
            }
        }

        return timeString
    }

    private fun parseHourMinute(timeString: String): Pair<Int, Int> {
        val trimmed = timeString.trim()
        val is12Hour = trimmed.contains("am", ignoreCase = true) || trimmed.contains("pm", ignoreCase = true)
        if (is12Hour) {
            try {
                val sdf = SimpleDateFormat("h:mm a", Locale.US)
                val date = sdf.parse(trimmed)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }
        // Fallback 24-hour "HH:mm"
        val parts = trimmed.split(":")
        val hour = parts.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
}
