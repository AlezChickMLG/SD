package org.example.encryption

import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Service
class PasswordHashing {
    private val algorithm = "PBKDF2WithHmacSHA512"
    private val iterations = 120_000
    private val keyLength = 256
    private val secret = "SomeRandomSecret"

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    fun String.hexToByteArray(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    fun generateRandomSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun generateHash(password: String, salt: ByteArray): String {
        val combinedSalt = salt + secret.toByteArray()

        val factory = SecretKeyFactory.getInstance(algorithm)
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(),
            combinedSalt,
            iterations,
            keyLength
        )

        val hash = factory.generateSecret(spec).encoded
        return hash.toHexString()
    }

    fun verifyPassword(password: String, salt: ByteArray, expectedHash: String): Boolean {
        val computedHash = generateHash(password, salt)
        println("computedHash: $computedHash\n")
        return computedHash == expectedHash
    }
}