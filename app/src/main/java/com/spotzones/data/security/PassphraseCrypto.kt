package com.spotzones.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-based AES-256-GCM encryption for backup files.
 *
 * Format (all Base64, ':'-joined): `v1:salt:iv:ciphertext`. The key is derived with PBKDF2-HMAC-SHA256
 * (210k iterations, per current OWASP guidance) so a leaked backup file is useless without the
 * passphrase. GCM provides authentication, so tampering or a wrong passphrase fails loudly on decrypt.
 */
object PassphraseCrypto {

    private const val VERSION = "v1"
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12

    fun encrypt(plaintext: String, passphrase: CharArray): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return listOf(VERSION, salt.b64(), iv.b64(), ciphertext.b64()).joinToString(":")
    }

    /** @throws IllegalArgumentException on malformed input, wrong passphrase or tampering. */
    fun decrypt(payload: String, passphrase: CharArray): String {
        val parts = payload.split(":")
        require(parts.size == 4 && parts[0] == VERSION) { "Unrecognised backup format" }
        val salt = parts[1].unb64()
        val iv = parts[2].unb64()
        val ciphertext = parts[3].unb64()
        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.unb64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
}
