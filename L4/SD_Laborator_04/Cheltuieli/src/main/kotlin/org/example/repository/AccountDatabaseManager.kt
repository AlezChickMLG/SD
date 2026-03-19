package org.example.repository

import org.example.encryption.PasswordHashing
import org.example.org.example.encryption.AesService
import org.example.org.example.pojo.BugetAccount
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

    fun storeBugetAccount(bugetAccount: BugetAccount): Boolean {
        val sql = "INSERT INTO buget (hashedUsername, bugetTotal, intretinere, " +
                "mancare, distractie, scoala, personale) VALUES (?, ?, ?, ?, ?, ?, ?)"

        val hashedUsername = passwordHashing.sha256(bugetAccount.username)
        println("[storeBugetAccount]: hashedUsername[${bugetAccount.username}]: $hashedUsername\n")

        return try {
            val result = jdbcTemplate.update(sql, hashedUsername, bugetAccount.bugetTotal,
                bugetAccount.intretinere, bugetAccount.mancare, bugetAccount.distractie,
                bugetAccount.scoala, bugetAccount.personale)
            result == 1
        } catch (e: DataAccessException) {
            println("Eroare la stocarea contului de buget: $e")
            false
        }
    }

    fun getBugetAccount(username: String): BugetAccount? {
        val sql = "SELECT * FROM buget WHERE hashedUsername = ?"

        val hashedUsername = passwordHashing.sha256(username)
        println("[getBugetAccount]: hashedUsername[$username]: $hashedUsername\n")

        return try {
             val result = jdbcTemplate.queryForObject<BugetAccount>(sql, arrayOf(hashedUsername)) { rs, _ ->
                BugetAccount(
                    username = username,
                    bugetTotal = rs.getDouble("bugetTotal"),
                    intretinere = rs.getDouble("intretinere"),
                    mancare = rs.getDouble("mancare"),
                    distractie = rs.getDouble("distractie"),
                    scoala = rs.getDouble("scoala"),
                    personale = rs.getDouble("personale")
                )
            }
            result
        } catch (e: DataAccessException) {
            println("Eroare la gasirea contului de buget: $e")
            null
        }
    }
}