package org.example.services

import org.example.encryption.PasswordHashing
import org.example.pojo.Account
import org.example.services.LoginService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LoginService (
    private val accountManager: AccountManager,
    private val passwordHashing: PasswordHashing
) {
    fun validateLogin(username: String, password: String): Boolean {
        val account = accountManager.search(username) ?: return false
        print("cont gasit:\nhash: ${account.password}\nsalt: ${account.salt}\n")
        return passwordHashing.verifyPassword(password, account.salt.hexToByteArray(), account.password)
    }
}