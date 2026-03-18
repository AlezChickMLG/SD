package org.example.services

import org.example.pojo.Account
import org.example.services.LoginService
import org.springframework.stereotype.Service

@Service
class LoginService (
    private val accountManager: AccountManager
) {
    fun validateLogin(username: String, password: String): Boolean {
        val account = accountManager.search(username) ?: return false

        if (account.password == password)
            return true

        return false
    }
}