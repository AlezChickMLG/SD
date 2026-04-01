package libraryapp.libraryappcachespring.business.models

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Cache (
    var timestamp: Int = 0,
    var query: String,
    var result: String
) {
    init {
        getTimestamp()
    }

    private fun getTimestamp() {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        timestamp =  now.format(formatter).split(":").joinToString("").toInt()
    }
}
