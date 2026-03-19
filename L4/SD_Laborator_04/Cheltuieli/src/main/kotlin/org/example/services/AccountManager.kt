package org.example.services

import org.example.org.example.pojo.BugetAccount
import org.example.repository.AccountDatabaseManager
import org.example.pojo.Account
import org.springframework.stereotype.Service

@Service
open class AccountManager (
    private var accountDatabaseManager: AccountDatabaseManager
) {
    fun search(username: String): Account? {
        return accountDatabaseManager.getAccountEncrypted(username)
    }

    fun loadBugetAccount(username: String): BugetAccount {
        var bugetAccount = accountDatabaseManager.getBugetAccount(username)
        if (bugetAccount == null) {
            bugetAccount = BugetAccount(username, 10000.0)
            accountDatabaseManager.storeBugetAccount(bugetAccount)
            return bugetAccount
        }
        return bugetAccount
    }
}