package org.nooklin.photos.ui.hidden

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// BiometricPrompt with BIOMETRIC_STRONG or DEVICE_CREDENTIAL, the fallback is the phone's own lock
@Composable
fun HiddenFolderGate(
    title: String = "Unlock Hidden Section",
    onAuthenticated: () -> Unit,
    onCancelled: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var statusText by remember { mutableStateOf("Unlocking...") }
    var failed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryCount) {
        if (activity == null) {
            onCancelled()
            return@LaunchedEffect
        }
        failed = false
        statusText = "Unlocking..."
        val executor = ContextCompat.getMainExecutor(activity)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    statusText = errString.toString()
                    failed = true
                }
            }
        )
        prompt.authenticate(promptInfo)
    }

    Box(
        Modifier
            .fillMaxSize()
            // Explicit background: otherwise the window background (white) ignores the Dark and Black themes
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(statusText, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Tap to retry",
                    color = Color(0xFF0A84FF),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { retryCount++ }
                )
                Text(
                    "Cancel",
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clickable { onCancelled() }
                )
            }
        } else {
            // No Surface ancestor, so Text needs an explicit theme color
            Text(statusText, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
