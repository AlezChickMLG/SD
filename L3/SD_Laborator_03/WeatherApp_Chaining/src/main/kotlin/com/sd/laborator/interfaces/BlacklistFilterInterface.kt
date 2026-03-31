package com.sd.laborator.interfaces

interface BlacklistFilterInterface {
    fun isBlacklisted(location: String): Boolean
}