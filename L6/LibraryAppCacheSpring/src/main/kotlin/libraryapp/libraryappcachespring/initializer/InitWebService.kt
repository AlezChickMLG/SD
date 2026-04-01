package libraryapp.libraryappcachespring.initializer

import libraryapp.libraryappcachespring.business.services.WebService
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class InitWebService (
    private val webService: WebService
) : CommandLineRunner {
    override fun run(vararg args: String) {
        webService.webClient()
        println("A fost pornit web clientul")
    }
}