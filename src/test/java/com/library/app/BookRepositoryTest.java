
package com.library.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BookRepositoryTest {

    @Autowired
    BookRepository repository;

    @Test
    void saveAssignsGeneratedId() {
        Book book = new Book("Clean Code", "978-0132350884");
        assertThat(book.getId()).isNull();

        Book saved = repository.save(book);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void findUpdateAndDelete() {
        Long id = repository.save(new Book("Refactoring", "978-0201485677")).getId();

        Book found = repository.findById(id).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Refactoring");

        found.setPublishedYear(1999);
        repository.save(found);
        assertThat(repository.findById(id).orElseThrow().getPublishedYear())
                .isEqualTo(1999);

        repository.deleteById(id);
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void findsByIsbn() {
        repository.save(new Book("Effective Java", "978-0134685991"));

        assertThat(repository.findByIsbn("978-0134685991"))
                .isPresent()
                .get().extracting(Book::getTitle).isEqualTo("Effective Java");
        assertThat(repository.findByIsbn("000-0000000000")).isEmpty();
    }

    @Test
    void findsByTitleFragmentIgnoringCase() {
        repository.save(new Book("Clean Code", "978-0132350884"));
        repository.save(new Book("Clean Architecture", "978-0134494166"));
        repository.save(new Book("Refactoring", "978-0201485677"));

        assertThat(repository.findByTitleContainingIgnoreCase("clean"))
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Clean Code", "Clean Architecture");
    }

    @Test
    void findsBooksPublishedAfterYear() {
        Book old = new Book("Refactoring", "978-0201485677");
        old.setPublishedYear(1999);
        Book recent = new Book("Effective Java", "978-0134685991");
        recent.setPublishedYear(2018);
        repository.save(old);
        repository.save(recent);

        assertThat(repository.findByPublishedYearGreaterThan(2000))
                .extracting(Book::getTitle)
                .containsExactly("Effective Java");
    }

    @Test
    void rejectsDuplicateIsbn() {
        repository.saveAndFlush(
                new Book("Clean Code", "978-0132350884"));

        assertThatThrownBy(() ->
                repository.saveAndFlush(
                        new Book("Copy", "978-0132350884")))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(
                        "UNIQUE constraint failed: book.isbn");
    }

    @Test
    void rejectsMissingTitle() {
        assertThatThrownBy(() ->
                repository.saveAndFlush(
                        new Book(null, "978-0132350884")))
                .isInstanceOf(DataAccessException.class);
    }
}

