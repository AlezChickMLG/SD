package org.example.org.example.encryption

import org.springframework.stereotype.Service
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@Service
class AesService {
    fun generateAESKey(keySize: Int = 256): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(keySize)
        return keyGenerator.generateKey()
    }

    fun aesEncrypt(data: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    fun decrypt(data: ByteArray, key: SecretKey): ByteArray {
        val iv = data.copyOfRange(0, 16)
        val encrypted = data.copyOfRange(16, data.size)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(encrypted)
    }
}