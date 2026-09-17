package com.example.thornburydental

import android.app.Application

class ThornburyApplication : Application() {

    companion object {
        lateinit var instance: ThornburyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.thornburydental.data.db.LocalDatabaseManager.initialize(this)
        com.example.thornburydental.data.DentalRepository.initializeFromDatabase()
        com.example.thornburydental.reminder.ReminderManager.createNotificationChannels(this)
    }
}
