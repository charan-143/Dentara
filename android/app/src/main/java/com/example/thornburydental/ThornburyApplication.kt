package com.example.thornburydental

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.thornburydental.data.security.AppSessionLifecycleObserver

class ThornburyApplication : Application() {

    companion object {
        lateinit var instance: ThornburyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.example.thornburydental.data.db.LocalDatabaseManager.initialize(this)
        com.example.thornburydental.data.DentalRepository.initializePreferencesSynchronously(this)
        com.example.thornburydental.data.DentalRepository.initializeFromDatabase()
        com.example.thornburydental.reminder.ReminderManager.createNotificationChannels(this)
        com.example.thornburydental.speech.model.SpeechModelProvider.initialize(this)

        // Register process lifecycle observer for app session locking
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppSessionLifecycleObserver)
    }
}
