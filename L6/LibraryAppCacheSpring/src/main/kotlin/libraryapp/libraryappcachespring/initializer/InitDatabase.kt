package libraryapp.libraryappcachespring.initializer

import libraryapp.libraryappcachespring.business.models.Book
import libraryapp.libraryappcachespring.business.models.Content
import libraryapp.libraryappcachespring.business.services.LibraryService

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class InitDatabase (
    private val libraryService: LibraryService,
    private val initialBooks: MutableSet<Book> = mutableSetOf(
        Book(
            Content(
                "Roberto Ierusalimschy",
                "Preface. When Waldemar, Luiz, and I started the " +
                        "development of Lua, back in 1993, we could hardly imagine that it " +
                        "would spread as it did . ...",
                "Programming in LUA",
                "Teora"
            )
        ),
        Book(
            Content(
                "Jules Verne",
                "Nemaipomeniti sunt francezii astia! - Vorbiti, domnule, va ascult !....",
                "Steaua Sudului",
                "Corint"
            )
        ),
        Book(
            Content(
                "Jules Verne",
                "Cuvant Inainte. Imaginatia copiilor - zicea un mare " +
                        "poet romantic spaniol - este asemenea unui cal nazdravan, iar " +
                        "curiozitatea lor e pintenul ce-l fugareste prin lumea celor mai " +
                        "indraznete proiecte.",
                "O calatorie spre centrul pamantului",
                "Polirom"
            )
        ),
        Book(
            Content(
                "Jules Verne",
                "Partea intai. Naufragiatii vazduhului. Capitolul 1. " +
                        "Uraganul din 1865. ...",
                "Insula Misterioasa",
                "Teora"
            )
        ),
        Book(
            Content(
                "Jules Verne",
                "Capitolul I. S-a pus un premiu pe capul unui om. Se " +
                        "ofera premiu de 2000 de lire ...",
                "Casa cu aburi",
                "Albatros"
            )
        )
    )
) : CommandLineRunner {
    override fun run(vararg args: String) {
        libraryService.createTable()
        println("Baza de date initializata")

        for (book in initialBooks) {
            if (book.author != null && book.name != null)
                if (libraryService.getBookByAuthorTitle(book.author!!, book.name!!) == null) {
                    libraryService.addBook(book)
                    println("Cartea ${book.name} scrisa de ${book.author} a fost adaugata")
                }

                else
                    println("Cartea ${book.name} scrisa de ${book.author} deja exista")
        }
    }
}