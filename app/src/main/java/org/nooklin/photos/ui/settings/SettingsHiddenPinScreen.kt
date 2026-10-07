package org.nooklin.photos.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Password
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.nooklin.photos.ui.hidden.HiddenPinEntryScreen
import org.nooklin.photos.ui.hidden.HiddenPinRecoveryCodeScreen
import org.nooklin.photos.ui.hidden.HiddenPinRecoveryEntryScreen
import org.nooklin.photos.ui.hidden.HiddenPinSetupScreen

private enum class Step { IDLE, VERIFY_CURRENT, RECOVERY_ENTRY, SETUP_NEW, REVEAL_CODE }
private enum class PendingAction { CHANGE, DISABLE }

// Changing or disabling needs the current PIN or recovery code. Recovery always forces a new PIN, so "forgot" alone can't switch the protection off.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHiddenPinScreen(
    pinSet: Boolean,
    verifyPin: suspend (String) -> Boolean,
    verifyRecoveryCode: suspend (String) -> Boolean,
    setNewPin: suspend (String) -> String,
    onClearPin: suspend () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableStateOf(Step.IDLE) }
    var pendingAction by remember { mutableStateOf(PendingAction.CHANGE) }
    var pendingCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    when (step) {
        Step.IDLE -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Extra PIN", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                Text(
                    "An extra layer of protection for the Hidden Section, separate from your device's own lock screen — you'll be asked for this PIN every time you open the Hidden Section, on top of, not instead of, the usual unlock.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                Text(
                    "When you set a PIN, a one-time recovery code is generated in case you forget it. It's shown once, right after setup — save it somewhere safe, since it can't be shown again.",
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                )
                if (!pinSet) {
                    SettingsGroup(
                        "Set up",
                        listOf({ SettingsNavRow("Set PIN", Icons.Filled.Password) { step = Step.SETUP_NEW } })
                    )
                } else {
                    SettingsGroup(
                        "Manage",
                        listOf(
                            {
                                SettingsNavRow("Change PIN", Icons.Filled.Password) {
                                    pendingAction = PendingAction.CHANGE
                                    step = Step.VERIFY_CURRENT
                                }
                            },
                            {
                                SettingsNavRow("Turn Off", Icons.Filled.Password) {
                                    pendingAction = PendingAction.DISABLE
                                    step = Step.VERIFY_CURRENT
                                }
                            }
                        )
                    )
                }
            }
        }
        Step.VERIFY_CURRENT -> HiddenPinEntryScreen(
            verify = verifyPin,
            onVerified = {
                when (pendingAction) {
                    PendingAction.CHANGE -> step = Step.SETUP_NEW
                    PendingAction.DISABLE -> scope.launch { onClearPin(); step = Step.IDLE }
                }
            },
            onForgotPin = { step = Step.RECOVERY_ENTRY },
            onCancel = { step = Step.IDLE }
        )
        Step.RECOVERY_ENTRY -> HiddenPinRecoveryEntryScreen(
            verify = verifyRecoveryCode,
            onVerified = { step = Step.SETUP_NEW },
            onCancel = { step = Step.IDLE }
        )
        Step.SETUP_NEW -> HiddenPinSetupScreen(
            onPinChosen = { pin ->
                scope.launch {
                    pendingCode = setNewPin(pin)
                    step = Step.REVEAL_CODE
                }
            },
            onCancel = { step = Step.IDLE }
        )
        Step.REVEAL_CODE -> {
            val code = pendingCode
            if (code != null) {
                HiddenPinRecoveryCodeScreen(
                    code = code,
                    onAcknowledged = {
                        pendingCode = null
                        step = Step.IDLE
                    }
                )
            }
        }
    }
}
