package com.library.app;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.List;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@RestController
public class HomeController {

    private final GreetingService greetingService;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;

    public HomeController(
            GreetingService greetingService,
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LoanRepository loanRepository) {

        this.greetingService = greetingService;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.loanRepository = loanRepository;
    }

    @GetMapping("/")
    public String hello() {
        return "Hello, Library";
    }

    @GetMapping("/hello")
    public String helloName(
            @RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name;
    }

    @GetMapping("/greet/{name}")
    public String greet(@PathVariable String name) {
        return "Welcome to the library, " + name;
    }

    public record AppInfo(String name, String version) {}

    @GetMapping("/info")
    public AppInfo info() {
        return new AppInfo("library-app", "0.0.1");
    }

    @GetMapping("/books")
    public List<BookDto> books() {
        return bookRepository.findAll()
                .stream()
                .map(BookDto::from)
                .toList();
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<BookDto> getBook(
            @PathVariable Long id) {

        return bookRepository.findById(id)
                .map(BookDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/books")
    public ResponseEntity<BookDto> createBook(
            @RequestParam String title,
            @RequestParam String isbn) {

        if (bookRepository.findByIsbn(isbn).isPresent()) {
            return ResponseEntity.status(409).build();
        }

        try {
            Book book = new Book(title, isbn);
            Book saved = bookRepository.saveAndFlush(book);

            return ResponseEntity
                    .created(URI.create("/books/" + saved.getId()))
                    .body(BookDto.from(saved));

        } catch (DataAccessException e) {
            if (e.getMessage() != null &&
                    e.getMessage().contains(
                            "UNIQUE constraint failed: book.isbn")) {
                return ResponseEntity.status(409).build();
            }

            throw e;
        }
    }

    @GetMapping("/greeting")
    public String greeting(
            @RequestParam(defaultValue = "World") String name) {
        return greetingService.greet(name);
    }

    @GetMapping("/members/summary")
    @Transactional(readOnly = true)
    public List<String> memberSummary() {
        return memberRepository.findAll().stream()
                .map(m -> m.getName() + ": " + m.getLoans().size())
                .toList();
    }

    @GetMapping("/books/by-author")
    public List<BookDto> booksByAuthor(
            @RequestParam Long authorId) {

        return bookRepository.findByAuthorsId(authorId)
                .stream()
                .map(BookDto::from)
                .toList();
    }

    @GetMapping("/books/by-author-name")
    public List<BookDto> booksByAuthorName(
            @RequestParam String lastName) {

        return bookRepository
                .findByAuthorsLastNameIgnoreCase(lastName)
                .stream()
                .map(BookDto::from)
                .toList();
    }

    @GetMapping("/members/{id}/loans/active")
    @Transactional(readOnly = true)
    public List<LoanDto> activeLoans(
            @PathVariable Long id) {

        return loanRepository
                .findByMemberIdAndReturnDateIsNull(id)
                .stream()
                .map(LoanDto::from)
                .toList();
    }
}