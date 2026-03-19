package org.example.services

import org.example.repository.AccountDatabaseManager
import org.example.pojo.Account
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class AccountManager (
    private var accountDatabaseManager: AccountDatabaseManager
) {
    fun search(username: String): Account? {
        return accountDatabaseManager.getAccountEncrypted(username)
    }
}