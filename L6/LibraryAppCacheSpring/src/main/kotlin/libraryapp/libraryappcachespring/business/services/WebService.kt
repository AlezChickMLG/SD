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

    override fun getFind(filterName: String, filter: String): String? {
        if (!listOf("author", "title", "publisher").contains(filterName)) {
            println("Nu exista $filterName in lista")
            return null
        }

        return webClient
            .get()
            .uri("http://localhost:8080/find?$filterName=$filter")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
    }
}