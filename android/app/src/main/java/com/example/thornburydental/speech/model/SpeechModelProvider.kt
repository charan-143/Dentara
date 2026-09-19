package com.example.thornburydental.speech.model

import android.content.Context
import java.io.File

/**
 * Process-wide holder for the model store.
 *
 * Deliberately a singleton: voice charting is currently mounted from two different screens,
 * and giving each its own store would let two concurrent downloads of the same 57 MB file
 * race over one destination file. Initialised from ThornburyApplication.
 */
object SpeechModelProvider {

    private const val MODELS_DIR_NAME = "speech-models"

    @Volatile
    private var store: SpeechModelStore? = null

    fun initialize(context: Context) {
        if (store != null) return
        synchronized(this) {
            if (store == null) {
                // Internal app storage: removed on uninstall, unreadable by other apps,
                // and needs no storage permission.
                store = SpeechModelStore(File(context.applicationContext.filesDir, MODELS_DIR_NAME))
            }
        }
    }

    /** Null until [initialize] has run. */
    fun storeOrNull(): SpeechModelStore? = store
}
