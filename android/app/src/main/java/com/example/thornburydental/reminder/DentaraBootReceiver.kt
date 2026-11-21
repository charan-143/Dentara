package com.example.thornburydental.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver dedicated solely to handling device reboots (BOOT_COMPLETED).
 */
class DentaraBootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DentaraBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted. Rescheduling all active reminders.")
            ReminderManager.rescheduleAllActiveReminders(context)
        }
    }
}
