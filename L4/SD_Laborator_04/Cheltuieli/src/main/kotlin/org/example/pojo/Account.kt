    package org.example.pojo

    data class Account (
        var username: String,
        var password: String,
        var salt: String = ""
    )