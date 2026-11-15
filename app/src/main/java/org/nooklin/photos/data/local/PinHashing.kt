package org.nooklin.photos.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHashing {
    private const val SALT_BYTES = 16
    private const val ITERATIONS = 100_000
    private const val PREFIX = "pbkdf2"

    // new format: pbkdf2:iterations:salt:hash. Old "salt:hash" values (one SHA-256 round) still verify until the PIN is set again
    fun hashWithNewSalt(value: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return "$PREFIX:$ITERATIONS:${salt.toHex()}:${pbkdf2(salt, value, ITERATIONS)}"
    }

    fun matches(value: String, stored: String): Boolean {
        val parts = stored.split(":")
        val (salt, iterations, expected) = if (parts.firstOrNull() == PREFIX) {
            Triple(parts.getOrNull(2)?.hexToBytesOrNull(), parts.getOrNull(1)?.toIntOrNull(), parts.getOrNull(3))
        } else {
            Triple(parts.getOrNull(0)?.hexToBytesOrNull(), 0, parts.getOrNull(1))
        }
        if (salt == null || iterations == null || expected == null) return false
        val actual = if (iterations == 0) sha256(salt, value) else pbkdf2(salt, value, iterations)
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }

    fun generateRecoveryCode(): String {
        val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val random = SecureRandom()
        val chars = CharArray(8) { alphabet[random.nextInt(alphabet.length)] }
        return "${String(chars, 0, 4)}-${String(chars, 4, 4)}"
    }

    private fun pbkdf2(salt: ByteArray, value: String, iterations: Int): String {
        val spec = PBEKeySpec(value.toCharArray(), salt, iterations, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex()
    }

    private fun sha256(salt: ByteArray, value: String): String =
        MessageDigest.getInstance("SHA-256").digest(salt + value.toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? = try {
        ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    } catch (e: Exception) {
        null
    }
}
