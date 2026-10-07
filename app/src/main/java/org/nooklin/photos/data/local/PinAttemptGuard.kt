package org.nooklin.photos.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

// PIN and recovery attempts share one counter. Uses the wall clock, so setting the clock back shortens a wait.
object PinAttemptGuard {
    private val waitsMs = longArrayOf(30_000L, 60_000L, 300_000L, 900_000L)
    private const val FREE_ATTEMPTS = 3

    private val _lockedUntil = MutableStateFlow(0L)
    val lockedUntil: StateFlow<Long> = _lockedUntil

    suspend fun load(preferences: AppPreferences) {
        _lockedUntil.value = preferences.pinLockedUntil.first()
    }

    fun remainingMs(now: Long = System.currentTimeMillis()): Long =
        (_lockedUntil.value - now).coerceAtLeast(0L)

    suspend fun registerFailure(preferences: AppPreferences) {
        val attempts = preferences.pinFailedAttempts.first() + 1
        val until = if (attempts >= FREE_ATTEMPTS) {
            System.currentTimeMillis() + waitsMs[(attempts - FREE_ATTEMPTS).coerceAtMost(waitsMs.lastIndex)]
        } else {
            0L
        }
        preferences.setPinAttemptState(attempts, until)
        _lockedUntil.value = until
    }

    suspend fun registerSuccess(preferences: AppPreferences) {
        preferences.setPinAttemptState(0, 0L)
        _lockedUntil.value = 0L
    }
}
