package com.example.thornburydental.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.thornburydental.MainActivity
import com.example.thornburydental.R
import com.example.thornburydental.data.db.LocalDatabaseManager
import com.example.thornburydental.util.todayIsoDate

/**
 * BroadcastReceiver for handling:
 * 1. Morning schedule briefing alarm: aggregates today's appointments and notifies clinician.
 * 2. Chairside patient arrival alert: notifies clinician before a scheduled patient arrives.
 * 3. Device reboot (BOOT_COMPLETED): reschedules all active alarms.
 */
class DentaraAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DentaraAlarmReceiver"
        private const val NOTIFICATION_ID_MORNING = 8001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Alarm triggered with action: $action")

        // Ensure database manager is initialized in this process
        if (!LocalDatabaseManager.isInitialized) {
            LocalDatabaseManager.initialize(context)
        }

        when (action) {
            ReminderManager.ACTION_DAILY_MORNING_BRIEFING -> {
                handleMorningBriefing(context)
            }
            ReminderManager.ACTION_PATIENT_ARRIVAL -> {
                handlePatientArrival(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device booted. Rescheduling all active reminders.")
                ReminderManager.rescheduleAllActiveReminders(context)
            }
        }
    }

    private fun handleMorningBriefing(context: Context) {
        val today = todayIsoDate()
        val appointments = try {
            LocalDatabaseManager.appointmentDao.getAppointmentsForDate(today)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching today's appointments for morning briefing", e)
            emptyList()
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Intent to launch app on Today tab
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "today")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MORNING,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (appointments.isNotEmpty()) {
            "Today's Schedule • ${appointments.size} Patient${if (appointments.size > 1) "s" else ""}"
        } else {
            "Today's Schedule • No Appointments"
        }

        val summaryText = if (appointments.isNotEmpty()) {
            appointments.joinToString(separator = "\n") { appt ->
                "• ${appt.time}: ${appt.patientName} (${appt.procedure})"
            }
        } else {
            "You have no chairside appointments scheduled for today."
        }

        val shortText = if (appointments.isNotEmpty()) {
            appointments.take(3).joinToString("; ") { "${it.time} ${it.patientName}" }
        } else {
            "No appointments scheduled for today."
        }

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_DAILY_BRIEFING)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(shortText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_MORNING, notification)
        Log.d(TAG, "Daily morning briefing notification posted for $today with ${appointments.size} appointments.")

        // Reschedule for tomorrow morning
        val prefs = LocalDatabaseManager.userPreferencesDao.getPreferences()
        val morningTime = prefs?.morningReminderTime ?: "08:00"
        if (prefs == null || prefs.morningReminderEnabled) {
            ReminderManager.scheduleDailyMorningBriefing(context, morningTime)
        }
    }

    private fun handlePatientArrival(context: Context, intent: Intent) {
        val apptId = intent.getStringExtra(ReminderManager.EXTRA_APPT_ID) ?: return
        val patientName = intent.getStringExtra(ReminderManager.EXTRA_PATIENT_NAME) ?: "Patient"
        val procedure = intent.getStringExtra(ReminderManager.EXTRA_PROCEDURE) ?: "Dental Procedure"
        val time = intent.getStringExtra(ReminderManager.EXTRA_TIME) ?: ""
        val leadMin = intent.getIntExtra(ReminderManager.EXTRA_LEAD_MIN, 15)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "today")
            putExtra("appointment_id", apptId)
        }
        val notificationId = apptId.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Patient Arriving Soon • $patientName"
        val content = "$patientName is scheduled at $time for $procedure ($leadMin min reminder)."

        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_PATIENT_ARRIVAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Patient arrival notification posted for $patientName (Appt: $apptId)")
    }
}
