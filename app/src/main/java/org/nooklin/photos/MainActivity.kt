package org.nooklin.photos

import android.os.Bundle
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
        enableEdgeToEdge()
        setContent {
            NooklinPhotosApp()
        }
    }

    private companion object {
        const val HIDDEN_RELOCK_GRACE_MS = 15_000L
    }
}
