package org.example.services

import org.example.encryption.PasswordHashing
import org.example.org.example.encryption.AesService
import org.example.pojo.Account
import org.example.services.LoginService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LoginService (
    private val accountManager: AccountManager,
    private val passwordHashing: PasswordHashing,
    private val aesService: AesService
) {
    fun validateLogin(account: Account): Boolean {
        val foundAccount = accountManager.search(account.username) ?: return false
        println("[validate login]:\ncont gasit:\nusername: ${foundAccount.username}\npassword: ${foundAccount.password}\nsalt: ${foundAccount.salt}\n")
        return passwordHashing.verifyPassword(account.password, foundAccount.salt.hexToByteArray(), foundAccount.password)
    }
}