package org.example.controllers

import org.example.encryption.PasswordHashing
import org.example.org.example.encryption.AesService
import org.example.org.example.pojo.BugetAccount
import org.example.repository.AccountDatabaseManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.example.pojo.Account
import org.example.services.AccountManager
import org.example.services.LoginService
import org.springframework.web.bind.annotation.PathVariable

/*@Controller
class PageController {
    @GetMapping(value = ["/"])
    fun index(): String {
        return "index"
    }

    @GetMapping(value = ["/home"])
    fun home(): String {
        return "home"
    }
}*/

@RestController
class LoginController (
    private val loginService: LoginService,
    private val accountManager: AccountManager,
    private val accountDatabaseManager: AccountDatabaseManager,
    private val passwordHashing: PasswordHashing,
    private val aesService: AesService,
) {
    @PostMapping(value = ["/login"])
    fun login(@RequestBody account: Account): ResponseEntity<Account> {
        println("[Login]:\nusername: ${account.username}\npassword: ${account.password}\n")
        if (loginService.validateLogin(account)) {
            return ResponseEntity(account, HttpStatus.OK)
        }
        return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @PostMapping(value = ["/register"])
    fun register(@RequestBody account: Account): ResponseEntity<Account> {
        println("[Register]:\nusername: ${account.username}\npassword: ${account.password}\n")
        if (accountDatabaseManager.getAccountEncrypted(account.username) == null) {
            println("[Inside-if]: Cont negasit\n")
            val salt = passwordHashing.generateRandomSalt()
            val hash = passwordHashing.generateHash(account.password, salt)

            val response = accountDatabaseManager.createAccount(account.username, hash, salt.toHexString())
            account.password = hash
            account.salt = salt.toHexString()

            if (response)
                return ResponseEntity(account, HttpStatus.CREATED)
            return ResponseEntity(account, HttpStatus.BAD_REQUEST)
        }
        println("[Outside-if]: Cont gasit\n")
        return ResponseEntity(account, HttpStatus.BAD_REQUEST)
    }

    @GetMapping(value = ["/home/{username}"])
    fun home(@PathVariable username: String): ResponseEntity<BugetAccount> {
        val bugetAccount: BugetAccount = accountManager.loadBugetAccount(username)
        return ResponseEntity(bugetAccount, HttpStatus.OK)
    }
}