package org.nooklin.photos.ui.hidden

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.nooklin.photos.ui.common.GlassPill
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import org.nooklin.photos.data.local.PinAttemptGuard

private val AccentBlue = Color(0xFF0A84FF)
private const val MIN_PIN_LENGTH = 4

private fun String.isValidPinFormat() = length >= MIN_PIN_LENGTH && all { it.isDigit() }

// Shared full-screen frame, the background follows the theme (no Surface ancestor)
@Composable
private fun PinScreenFrame(
    title: String,
    body: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
            if (body != null) {
                Text(
                    body,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
            content()
        }
    }
}

/** Seconds left on the failed-attempt lockout, refreshed twice a second, 0 when attempts are allowed. */
@Composable
private fun rememberPinLockSeconds(): Int {
    val lockedUntil by PinAttemptGuard.lockedUntil.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockedUntil) {
        while (System.currentTimeMillis() < lockedUntil) {
            now = System.currentTimeMillis()
            delay(500)
        }
        now = System.currentTimeMillis()
    }
    return ((lockedUntil - now + 999L) / 1000L).coerceAtLeast(0L).toInt()
}

private fun pinLockMessage(seconds: Int): String =
    if (seconds >= 60) "Too many attempts. Try again in ${(seconds + 59) / 60} min" else "Too many attempts. Try again in $seconds s"

// Verifies the Extra PIN at the entry gate and before changing the PIN, so neither route bypasses the other
@Composable
fun HiddenPinEntryScreen(
    verify: suspend (String) -> Boolean,
    onVerified: () -> Unit,
    onForgotPin: () -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lockSeconds = rememberPinLockSeconds()

    PinScreenFrame(title = "Enter PIN", body = "Enter your Extra PIN for the Hidden Section.") {
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { pin = it; error = null } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = GlassPill.shape,
            isError = error != null,
            modifier = Modifier.width(200.dp)
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        if (lockSeconds > 0) {
            Text(pinLockMessage(lockSeconds), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            enabled = pin.isNotEmpty() && !checking && lockSeconds == 0,
            onClick = {
                checking = true
                scope.launch {
                    if (verify(pin)) {
                        onVerified()
                    } else {
                        error = "Incorrect PIN"
                        pin = ""
                        checking = false
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        ) {
            Text("Unlock")
        }
        Text(
            "Forgot PIN?",
            color = AccentBlue,
            modifier = Modifier.padding(top = 16.dp).clickable { onForgotPin() }
        )
        Text(
            "Cancel",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp).clickable { onCancel() }
        )
    }
}

// New PIN, entered twice to catch typos. Used after setup and after recovery.
@Composable
fun HiddenPinSetupScreen(
    onPinChosen: (String) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0 = first entry, 1 = confirm
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    PinScreenFrame(
        title = if (step == 0) "Set a PIN" else "Confirm PIN",
        body = if (step == 0) {
            "At least $MIN_PIN_LENGTH digits — 6 is recommended for better security."
        } else {
            "Enter the same PIN again."
        }
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8 && it.all { c -> c.isDigit() }) { pin = it; error = null } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = GlassPill.shape,
            isError = error != null,
            modifier = Modifier.width(200.dp)
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            enabled = pin.isNotEmpty(),
            onClick = {
                if (step == 0) {
                    if (!pin.isValidPinFormat()) {
                        error = "At least $MIN_PIN_LENGTH digits"
                    } else {
                        firstPin = pin
                        pin = ""
                        step = 1
                    }
                } else {
                    if (pin != firstPin) {
                        error = "Doesn't match, try again"
                        pin = ""
                        step = 0
                        firstPin = ""
                    } else {
                        onPinChosen(pin)
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        ) {
            Text(if (step == 0) "Next" else "Confirm")
        }
        Text(
            "Cancel",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp).clickable { onCancel() }
        )
    }
}

// Shown once after a PIN is set. Never stored in plaintext, losing it means no way back in.
@Composable
fun HiddenPinRecoveryCodeScreen(code: String, onAcknowledged: () -> Unit) {
    var acknowledged by remember { mutableStateOf(false) }
    // The code is shown once and the PIN is already changed, so back must not discard it.
    BackHandler(enabled = true) {}
    PinScreenFrame(
        title = "Save your recovery code",
        body = "If you forget your PIN, this is the only way back into the Hidden Section — Nooklin Photos can't reset it for you. Write it down or save it somewhere safe."
    ) {
        Text(
            code,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it })
            Text("I've saved this code somewhere safe", color = MaterialTheme.colorScheme.onBackground)
        }
        Button(
            enabled = acknowledged,
            onClick = onAcknowledged,
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

// "Forgot PIN?": a correct code always forces a new PIN
@Composable
fun HiddenPinRecoveryEntryScreen(
    verify: suspend (String) -> Boolean,
    onVerified: () -> Unit,
    onCancel: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lockSeconds = rememberPinLockSeconds()

    PinScreenFrame(
        title = "Enter recovery code",
        body = "Enter the recovery code you saved when you set up your PIN."
    ) {
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase(); error = null },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = GlassPill.shape,
            isError = error != null,
            modifier = Modifier.width(200.dp)
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        if (lockSeconds > 0) {
            Text(pinLockMessage(lockSeconds), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        Button(
            enabled = code.isNotBlank() && !checking && lockSeconds == 0,
            onClick = {
                checking = true
                scope.launch {
                    // Dash is a display/typing convenience, not part of the
                    // actual secret, so accept it with or without one.
                    if (verify(code.replace("-", "").trim())) {
                        onVerified()
                    } else {
                        error = "Incorrect recovery code"
                        checking = false
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
        ) {
            Text("Continue")
        }
        Text(
            "Cancel",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp).clickable { onCancel() }
        )
    }
}

private enum class PinGateStep { VERIFY, RECOVERY_ENTRY, NEW_PIN_SETUP, NEW_RECOVERY_CODE_REVEAL }

// Enter PIN, or recover and pick a new one. Reusable, callers supply verify and setNewPin.
@Composable
fun HiddenPinGate(
    verifyPin: suspend (String) -> Boolean,
    verifyRecoveryCode: suspend (String) -> Boolean,
    setNewPin: suspend (String) -> String,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableStateOf(PinGateStep.VERIFY) }
    var pendingNewCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    when (step) {
        PinGateStep.VERIFY -> HiddenPinEntryScreen(
            verify = verifyPin,
            onVerified = onUnlocked,
            onForgotPin = { step = PinGateStep.RECOVERY_ENTRY },
            onCancel = onCancel
        )
        PinGateStep.RECOVERY_ENTRY -> HiddenPinRecoveryEntryScreen(
            verify = verifyRecoveryCode,
            onVerified = { step = PinGateStep.NEW_PIN_SETUP },
            onCancel = { step = PinGateStep.VERIFY }
        )
        PinGateStep.NEW_PIN_SETUP -> HiddenPinSetupScreen(
            onPinChosen = { pin ->
                scope.launch {
                    pendingNewCode = setNewPin(pin)
                    step = PinGateStep.NEW_RECOVERY_CODE_REVEAL
                }
            },
            onCancel = onCancel
        )
        PinGateStep.NEW_RECOVERY_CODE_REVEAL -> {
            val code = pendingNewCode
            if (code != null) {
                HiddenPinRecoveryCodeScreen(code = code, onAcknowledged = onUnlocked)
            }
        }
    }
}
