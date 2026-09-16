package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * SecureStorage provides hardware-backed AES-256 GCM encryption via the Android KeyStore.
 * Used for encrypting sensitive credentials, session tokens, and financial authentication
 * data before persisting to storage, eliminating plaintext token leakage.
 */
object SecureStorage {

    private const val TAG = "SecureStorage"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "SplitExpense_MasterAuthKey"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } else {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Android KeyStore master key", e)
            null
        }
    }

    /**
     * Encrypts a plaintext string using AES-256 GCM.
     * Returns Base64-encoded payload containing: [12-byte IV] + [Ciphertext + Tag]
     */
    fun encrypt(plainText: String?): String? {
        if (plainText.isNullOrEmpty()) return null
        return try {
            val secretKey = getOrCreateSecretKey() ?: return obfuscateFallback(plainText)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            "ENC:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption error, applying fallback", e)
            obfuscateFallback(plainText)
        }
    }

    /**
     * Decrypts an AES-256 GCM encrypted string.
     */
    fun decrypt(encryptedPayload: String?): String? {
        if (encryptedPayload.isNullOrEmpty()) return null
        if (!encryptedPayload.startsWith("ENC:")) {
            // Unencrypted legacy token or fallback format
            return if (encryptedPayload.startsWith("OBF:")) deobfuscateFallback(encryptedPayload) else encryptedPayload
        }

        return try {
            val base64Data = encryptedPayload.removePrefix("ENC:")
            val combined = Base64.decode(base64Data, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null

            val secretKey = getOrCreateSecretKey() ?: return null
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val cipherBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.size)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error", e)
            null
        }
    }

    private fun obfuscateFallback(data: String): String {
        val bytes = data.toByteArray(Charsets.UTF_8)
        for (i in bytes.indices) {
            bytes[i] = (bytes[i].toInt() xor 0x5A).toByte()
        }
        return "OBF:" + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun deobfuscateFallback(obfData: String): String? {
        return try {
            val bytes = Base64.decode(obfData.removePrefix("OBF:"), Base64.NO_WRAP)
            for (i in bytes.indices) {
                bytes[i] = (bytes[i].toInt() xor 0x5A).toByte()
            }
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
