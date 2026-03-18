package org.example.controllers

import org.example.repository.AccountDatabaseManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.example.pojo.Account
import org.example.services.LoginService

@Controller
class PageController {
    @GetMapping(value = ["/"])
    fun index(): String {
        return "index"
    }

    @GetMapping(value = ["/home"])
    fun home(): String {
        return "home"
    }
}

@RestController
class LoginController {
    @Autowired
    private lateinit var loginService: LoginService

    @Autowired
    private lateinit var accountDatabaseManager: AccountDatabaseManager

    @PostMapping(value = ["/login"])
    fun login(@RequestBody account: Account): ResponseEntity<Account> {
        if (loginService.validateLogin(account.username, account.password)) {
            return ResponseEntity(account, HttpStatus.OK)
        }
        return ResponseEntity(HttpStatus.NOT_FOUND)
    }

    @PostMapping(value = ["/register"])
    fun register(@RequestBody account: Account): ResponseEntity<Account> {
        println("register ${account.username}")
        if (accountDatabaseManager.getAccount(account.username) == null) {
            println("Cont negasit")
            val response = accountDatabaseManager.createAccount(account.username, account.password)
            println("Raspuns: ${response}")
            if (response)
                return ResponseEntity(account, HttpStatus.CREATED)
            return ResponseEntity(account, HttpStatus.BAD_REQUEST)
        }

        return ResponseEntity(account, HttpStatus.BAD_REQUEST)
    }
}