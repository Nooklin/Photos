package org.nooklin.photos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.nooklin.photos.data.local.AppPreferences
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import org.nooklin.photos.ui.NooklinPhotosApp
import android.os.SystemClock
import androidx.activity.viewModels
import org.nooklin.photos.ui.hidden.HiddenViewModel

private object NoHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {}
}

class MainActivity : FragmentActivity() {
    private val hiddenViewModel: HiddenViewModel by viewModels()
    private var stoppedAtMs = 0L
    private val haptics by lazy { AppPreferences(applicationContext).hapticsEnabled }
    private var externalRequest by mutableStateOf<ExternalRequest?>(null)

    private var leftByUser = false
    private var startedByApp = false

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        startedByApp = true
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun startIntentSenderForResult(
        intent: android.content.IntentSender, requestCode: Int, fillInIntent: Intent?,
        flagsMask: Int, flagsValues: Int, extraFlags: Int, options: Bundle?
    ) {
        startedByApp = true
        super.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        leftByUser = true
    }

    override fun onStop() {
        super.onStop()
        stoppedAtMs = SystemClock.elapsedRealtime()
        if (leftByUser && !startedByApp && !isChangingConfigurations) hiddenViewModel.lock()
        leftByUser = false
        startedByApp = false
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAtMs != 0L && SystemClock.elapsedRealtime() - stoppedAtMs > HIDDEN_RELOCK_GRACE_MS) {
            hiddenViewModel.lock()
        }
        stoppedAtMs = 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppStrings.activity = java.lang.ref.WeakReference(this)
        externalRequest = intent.toExternalRequest(this)
        enableEdgeToEdge()
        setContent {
            val realHaptics = LocalHapticFeedback.current
            val hapticsOn by haptics.collectAsStateWithLifecycle(true)
            CompositionLocalProvider(LocalHapticFeedback provides if (hapticsOn) realHaptics else NoHaptics) {
                NooklinPhotosApp(externalRequest = externalRequest, onExternalFinish = ::finishExternal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalRequest = intent.toExternalRequest(this)
    }

    private fun finishExternal(uris: List<Uri>?) {
        if (uris.isNullOrEmpty()) {
            setResult(RESULT_CANCELED)
        } else {
            setResult(RESULT_OK, uris.toPickResult())
        }
        finish()
    }

    private companion object {
        const val HIDDEN_RELOCK_GRACE_MS = 15_000L
    }
}
