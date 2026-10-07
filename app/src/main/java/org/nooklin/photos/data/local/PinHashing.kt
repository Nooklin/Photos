package org.nooklin.photos.data.local

import java.security.MessageDigest
import java.security.SecureRandom

// Salted SHA-256, stored as "saltHex:hashHex"
object PinHashing {
    private const val SALT_BYTES = 16

    fun hashWithNewSalt(value: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "${salt.toHex()}:${digest(salt, value)}"
    }

    fun matches(value: String, stored: String): Boolean {
        val parts = stored.split(":")
        val saltHex = parts.getOrNull(0) ?: return false
        val expectedHash = parts.getOrNull(1) ?: return false
        val salt = saltHex.hexToBytesOrNull() ?: return false
        return digest(salt, value) == expectedHash
    }

    // Excludes 0/O/1/I to avoid typos
    fun generateRecoveryCode(): String {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val chars = CharArray(8) { alphabet[random.nextInt(alphabet.length)] }
        return "${String(chars, 0, 4)}-${String(chars, 4, 4)}"
    }

    private fun digest(salt: ByteArray, value: String): String =
        MessageDigest.getInstance("SHA-256").digest(salt + value.toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? = try {
        ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    } catch (e: Exception) {
        null
    }
}
