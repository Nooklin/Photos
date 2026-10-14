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
import org.nooklin.photos.R
import org.nooklin.photos.appString

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
                    title = { Text(appString(R.string.common_extra_pin), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appString(R.string.common_back))
                        }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                Text(
                    appString(R.string.settings_hidden_pin_an_extra_layer_of_protection),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                )
                Text(
                    appString(R.string.settings_hidden_pin_when_you_set_a_pin),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
                )
                if (!pinSet) {
                    SettingsGroup(
                        appString(R.string.settings_hidden_pin_set_up),
                        listOf({ SettingsNavRow(appString(R.string.settings_hidden_pin_set_pin), Icons.Filled.Password) { step = Step.SETUP_NEW } })
                    )
                } else {
                    SettingsGroup(
                        appString(R.string.settings_hidden_pin_manage),
                        listOf(
                            {
                                SettingsNavRow(appString(R.string.settings_hidden_pin_change_pin), Icons.Filled.Password) {
                                    pendingAction = PendingAction.CHANGE
                                    step = Step.VERIFY_CURRENT
                                }
                            },
                            {
                                SettingsNavRow(appString(R.string.common_turn_off), Icons.Filled.Password) {
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
