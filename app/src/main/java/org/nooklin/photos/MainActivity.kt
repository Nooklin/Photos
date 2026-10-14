package org.nooklin.photos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import org.nooklin.photos.ui.NooklinPhotosApp
import android.os.SystemClock
import androidx.activity.viewModels
import org.nooklin.photos.ui.hidden.HiddenViewModel

// FragmentActivity: BiometricPrompt needs it
class MainActivity : FragmentActivity() {
    private val hiddenViewModel: HiddenViewModel by viewModels()
    private var stoppedAtMs = 0L
    // Set when another app opened a file or asked for one, null for a normal launch
    private var externalRequest by mutableStateOf<ExternalRequest?>(null)

    override fun onStop() {
        super.onStop()
        stoppedAtMs = SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // System screens (biometric, pickers, delete prompts) also stop the activity, only a real absence locks
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
            NooklinPhotosApp(externalRequest = externalRequest, onExternalFinish = ::finishExternal)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalRequest = intent.toExternalRequest(this)
    }

    // Null or empty means cancelled (or a viewed file was closed)
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
