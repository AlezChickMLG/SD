package org.example.org.example.encryption

import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Base64

@Service
class AesService {
    private val keyPath: Path = Path.of("secret.key")

    fun getOrCreateKey(): SecretKey {
        return if (Files.exists(keyPath)) {
            loadKey()
        } else {
            val key = generateAESKey()
            saveKey(key)
            key
        }
    }

    private fun saveKey(secretKey: SecretKey) {
        val encoded = Base64.getEncoder().encodeToString(secretKey.encoded)
        Files.writeString(keyPath, encoded)
    }

    private fun loadKey(): SecretKey {
        val encoded = Files.readString(keyPath).trim()
        val decoded = Base64.getDecoder().decode(encoded)
        return SecretKeySpec(decoded, "AES")
    }

    fun generateAESKey(keySize: Int = 256): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keySize)
        return keyGenerator.generateKey()
    }

    fun aesEncrypt(data: ByteArray): ByteArray {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun aesDecrypt(data: ByteArray): ByteArray {
        val secretKey = getOrCreateKey()
        val iv = data.copyOfRange(0, 16)
        val encrypted = data.copyOfRange(16, data.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(encrypted)
    }
}