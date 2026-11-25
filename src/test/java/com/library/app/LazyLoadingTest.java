package com.library.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LazyLoadingTest {

    @Autowired BookRepository bookRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired LoanRepository loanRepository;

    Member ana;

    @BeforeEach
    void seed() {
        Book book = bookRepository.save(new Book("Clean Code", "978-0132350884"));
        ana = memberRepository.save(new Member("Ana", "ana@x.ro", LocalDate.of(2026, 9, 1)));
        loanRepository.save(new Loan(book, ana,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)));
    }

    @AfterEach
    void clean() {   // nothing is rolled back here, so delete by hand
        loanRepository.deleteAll();
        memberRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void lazyCollectionIsNotAvailableOutsideASession() {
        Member loaded = memberRepository.findAll().get(0);

        assertThatThrownBy(() -> loaded.getLoans().size())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void lazyManyToOneIsNotAvailableOutsideASession() {
        Loan loaded = loanRepository.findAll().get(0);

        assertThatThrownBy(() -> loaded.getBook().getTitle())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void entityGraphLoadsTheBookTogetherWithTheLoans() {
        Loan loaded = loanRepository
                .findByMemberIdAndReturnDateIsNull(ana.getId()).get(0);

        assertThat(loaded.getBook().getTitle()).isEqualTo("Clean Code");
    }
}
