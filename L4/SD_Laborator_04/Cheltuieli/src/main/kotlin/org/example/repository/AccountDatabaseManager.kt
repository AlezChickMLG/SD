package org.example.repository

import org.example.pojo.Account
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class AccountDatabaseManager (
    private val jdbcTemplate: JdbcTemplate
){
    fun getAccount(username: String): Account? {
        val sql = "SELECT password FROM account where username = ?"
        return try {
            jdbcTemplate.queryForObject(sql, arrayOf(username)) { rs, _ ->
                Account(
                    username = username,
                    password = rs.getString("password")
                )
            }
        } catch (e: DataAccessException) {
            null
        }
    }

    fun createAccount(username: String, password: String): Boolean {
        val sql = "INSERT INTO account (username, password) VALUES (?, ?)";

        return try {
            val rows = jdbcTemplate.update(sql, username, password)
            rows == 1
        } catch (e: DataAccessException) {
            false
        }
    }
}