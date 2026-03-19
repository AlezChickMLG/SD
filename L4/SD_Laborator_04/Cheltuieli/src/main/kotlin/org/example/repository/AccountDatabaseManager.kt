package org.example.repository

import org.example.encryption.PasswordHashing
import org.example.org.example.encryption.AesService
import org.example.pojo.Account
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class AccountDatabaseManager (
    private val jdbcTemplate: JdbcTemplate,
    private val aesService: AesService,
    private val passwordHashing: PasswordHashing
){
    fun getAccountEncrypted(username: String): Account? {
        val sql = "SELECT encryptedUsername, password, salt FROM account WHERE hashedUsername = ?"
        val hashedUsername = passwordHashing.sha256(username)

        println("[getAccountEncrypted]: hashedUsername[$username]: $hashedUsername\n")

        return try {
            jdbcTemplate.query(sql, arrayOf(hashedUsername)) { rs, _ ->
                Account(
                    username = String(
                        aesService.aesDecrypt(rs.getString("encryptedUsername").hexToByteArray()),
                        Charsets.UTF_8
                    ),
                    password = rs.getString("password"),
                    salt = rs.getString("salt")
                )
            }.firstOrNull()
        } catch (e: DataAccessException) {
            println("Eroare la getAccountEncrypted: $e")
            null
        }
    }

    fun createAccount(username: String, password: String, salt: String): Boolean {
        val sql = "INSERT INTO account (hashedUsername, encryptedUsername, password, salt) VALUES (?, ?, ?, ?)";
        val encryptedUsername = aesService.aesEncrypt(username.toByteArray()).toHexString()

        val hashedUsername = passwordHashing.sha256(username)
        println("[createAccount] -> $username: hashedUsername: $hashedUsername\nencryptedUsername: ${encryptedUsername}\npassword: $password\nsalt: $salt\n")

        return try {
            val rows = jdbcTemplate.update(sql, hashedUsername, encryptedUsername, password, salt)
            rows == 1
        } catch (e: DataAccessException) {
            println("Eroare la crearea contului: $e")
            false
        }
    }
}