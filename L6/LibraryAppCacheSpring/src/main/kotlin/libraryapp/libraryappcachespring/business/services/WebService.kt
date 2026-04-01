package libraryapp.libraryappcachespring.business.services

import libraryapp.libraryappcachespring.business.interfaces.cacheInterfaces.IWebService
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class WebService : IWebService {
    private lateinit var webClient: WebClient

    override fun webClient() {
        webClient = WebClient.create()
    }

    override fun getPrint(format: String): String? {
        return webClient
            .get()
            .uri("http://localhost:8080/print?format=$format")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }
}