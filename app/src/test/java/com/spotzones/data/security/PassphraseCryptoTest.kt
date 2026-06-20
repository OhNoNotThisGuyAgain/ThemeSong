package com.spotzones.data.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Robolectric is used because [PassphraseCrypto] depends on android.util.Base64. */
@RunWith(RobolectricTestRunner::class)
class PassphraseCryptoTest {

    private val plaintext = """{"zones":[{"name":"Home"}]}"""

    @Test fun `encrypt then decrypt returns original`() {
        val encrypted = PassphraseCrypto.encrypt(plaintext, "correct horse battery".toCharArray())
        val decrypted = PassphraseCrypto.decrypt(encrypted, "correct horse battery".toCharArray())
        assertThat(decrypted).isEqualTo(plaintext)
    }

    @Test fun `ciphertext is not the plaintext and is versioned`() {
        val encrypted = PassphraseCrypto.encrypt(plaintext, "pw".toCharArray())
        assertThat(encrypted).startsWith("v1:")
        assertThat(encrypted).doesNotContain("Home")
    }

    @Test fun `wrong passphrase fails to decrypt`() {
        val encrypted = PassphraseCrypto.encrypt(plaintext, "right".toCharArray())
        try {
            PassphraseCrypto.decrypt(encrypted, "wrong".toCharArray())
            assertThat(false).isTrue() // GCM auth tag must reject the wrong key
        } catch (e: Exception) {
            assertThat(e).isNotNull()
        }
    }

    @Test fun `each encryption uses a fresh salt and iv`() {
        val a = PassphraseCrypto.encrypt(plaintext, "pw".toCharArray())
        val b = PassphraseCrypto.encrypt(plaintext, "pw".toCharArray())
        assertThat(a).isNotEqualTo(b)
    }
}
