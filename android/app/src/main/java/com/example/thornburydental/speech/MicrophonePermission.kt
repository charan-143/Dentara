package com.example.thornburydental.speech

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Whether this device may currently record audio, and how to ask for it.
 *
 * @param permanentlyDenied the clinician declined and Android will no longer show the system
 *        dialog. Calling [request] then does nothing visible, so the UI has to send them to
 *        app settings rather than silently failing.
 */
@Immutable
class MicrophonePermissionState internal constructor(
    val isGranted: Boolean,
    val permanentlyDenied: Boolean,
    private val onRequest: () -> Unit
) {
    fun request() = onRequest()
}

/**
 * Tracks RECORD_AUDIO for voice dictation.
 *
 * Re-checks on resume, because the clinician may have granted the permission in system
 * settings and come back, and nothing else would tell us that.
 */
@Composable
fun rememberMicrophonePermissionState(
    onGranted: () -> Unit = {}
): MicrophonePermissionState {
    val context = LocalContext.current

    var isGranted by remember { mutableStateOf(context.hasRecordAudioPermission()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
        if (granted) {
            permanentlyDenied = false
            onGranted()
        } else {
            // After a refusal, if Android will no longer show a rationale it will not show
            // the dialog again either. Settings is the only remaining route.
            val activity = context.findActivity()
            permanentlyDenied = activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = context.hasRecordAudioPermission()
                if (granted != isGranted) {
                    isGranted = granted
                }
                if (granted) {
                    permanentlyDenied = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(isGranted, permanentlyDenied) {
        MicrophonePermissionState(
            isGranted = isGranted,
            permanentlyDenied = permanentlyDenied,
            onRequest = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
        )
    }
}

fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

/** Unwraps the Activity behind a Compose context, which may be a ContextWrapper. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
