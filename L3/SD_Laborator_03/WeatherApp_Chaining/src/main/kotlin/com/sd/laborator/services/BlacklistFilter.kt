package com.sd.laborator.services

import com.sd.laborator.interfaces.BlacklistFilterInterface
import org.springframework.stereotype.Service
import java.io.File

@Service
class BlacklistFilter : BlacklistFilterInterface {
    @Override
    override fun isBlacklisted(location: String): Boolean {
        val rawData = File("src/main/resources/blacklist.txt").readText().trim().split(", ")
        for (city in rawData) {
            if (city.trim() == location.trim()) {
                return true
            }
        }

        return false
    }
}