package com.library.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RelationshipsTest {

    @Autowired BookRepository bookRepository;
    @Autowired AuthorRepository authorRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    Author martin, fowler;
    Book cleanCode, refactoring;
    Member ana, dan;

    @BeforeEach
    void seed() {
        martin = authorRepository.save(new Author("Robert", "Martin"));
        fowler = authorRepository.save(new Author("Martin", "Fowler"));

        cleanCode = new Book("Clean Code", "978-0132350884");
        cleanCode.addAuthor(martin);
        refactoring = new Book("Refactoring", "978-0201485677");
        refactoring.addAuthor(fowler);
        refactoring.addAuthor(martin);
        bookRepository.save(cleanCode);
        bookRepository.save(refactoring);

        ana = memberRepository.save(new Member("Ana", "ana@x.ro", day(9, 1)));
        dan = memberRepository.save(new Member("Dan", "dan@x.ro", day(9, 2)));
    }

    static LocalDate day(int month, int day) {
        return LocalDate.of(2026, month, day);
    }

    int joinRows(Long bookId) {
        return jdbc.queryForObject(
                "select count(*) from book_author where book_id = ?",
                Integer.class, bookId);
    }

    @Test
    void owningSideWritesTheJoinRows() {
        em.flush();

        assertThat(joinRows(cleanCode.getId())).isEqualTo(1);
        assertThat(joinRows(refactoring.getId())).isEqualTo(2);
    }

    @Test
    void thereIsNoExtraJoinTable() {   // what "forgetting mappedBy" would create
        Integer tables = jdbc.queryForObject(
                "select count(*) from sqlite_master where name = 'member_loans'",
                Integer.class);

        assertThat(tables).isZero();
    }

    @Test
    void inverseSideAloneWritesNothing() {
        Book effective = bookRepository.save(new Book("Effective Java", "978-0134685991"));
        Author bloch = authorRepository.save(new Author("Joshua", "Bloch"));

        bloch.getBooks().add(effective);   // only the inverse side
        em.flush();

        assertThat(joinRows(effective.getId())).isZero();
    }

    @Test
    void savingABookCascadesToNewAuthors() {
        Author bloch = new Author("Joshua", "Bloch");   // not saved
        Book effective = new Book("Effective Java", "978-0134685991");
        effective.addAuthor(bloch);

        bookRepository.save(effective);
        bookRepository.flush();

        assertThat(bloch.getId()).isNotNull();
        assertThat(joinRows(effective.getId())).isEqualTo(1);
    }

    @Test
    void findsBooksByAuthor() {
        assertThat(bookRepository.findByAuthorsId(fowler.getId()))
                .extracting(Book::getTitle)
                .containsExactly("Refactoring");
        assertThat(bookRepository.findByAuthorsLastNameIgnoreCase("martin"))
                .extracting(Book::getTitle)
                .containsExactlyInAnyOrder("Clean Code", "Refactoring");
    }

    @Test
    void findsActiveLoansPerMember() {
        Loan returned = new Loan(cleanCode, ana, day(9, 3), day(9, 17));
        returned.setReturnDate(day(9, 10));
        Loan open = new Loan(refactoring, ana, day(10, 1), day(10, 15));
        Loan danLoan = new Loan(cleanCode, dan, day(10, 2), day(10, 16));
        ana.addLoan(returned);
        ana.addLoan(open);
        dan.addLoan(danLoan);
        loanRepository.saveAll(List.of(returned, open, danLoan));

        assertThat(loanRepository.findByMemberIdAndReturnDateIsNull(ana.getId()))
                .extracting(l -> l.getBook().getTitle())
                .containsExactly("Refactoring");
        assertThat(loanRepository.countByMemberIdAndReturnDateIsNull(ana.getId()))
                .isEqualTo(1L);
        assertThat(open.isActive()).isTrue();
        assertThat(returned.isActive()).isFalse();
    }

    @Test
    void loanWritesBothForeignKeys() {
        Loan loan = new Loan(cleanCode, ana, day(10, 1), day(10, 15));
        ana.addLoan(loan);
        loanRepository.save(loan);
        em.flush();

        Map<String, Object> row = jdbc.queryForMap(
                "select book_id, member_id from loan where id = ?", loan.getId());
        assertThat(((Number) row.get("book_id")).longValue()).isEqualTo(cleanCode.getId());
        assertThat(((Number) row.get("member_id")).longValue()).isEqualTo(ana.getId());
    }

    @Test
    void theLoansOfAMemberAreReadBackFromTheDatabase() {
        Loan loan = new Loan(cleanCode, ana, day(10, 1), day(10, 15));
        ana.addLoan(loan);
        loanRepository.save(loan);
        em.flush();
        em.clear();   // forget everything: the next read goes to the database

        Member reloaded = memberRepository.findById(ana.getId()).orElseThrow();

        assertThat(reloaded.getLoans()).hasSize(1);
    }

    @Test
    void loanWithoutMemberIsRejected() {
        assertThatThrownBy(() ->
                loanRepository.saveAndFlush(
                        new Loan(cleanCode, null, day(10, 1), day(10, 15))))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void deletingABookKeepsItsAuthors() {
        em.flush();
        em.clear();   // forget everything: the book is loaded again below
        Book loaded = bookRepository.findById(cleanCode.getId()).orElseThrow();

        bookRepository.delete(loaded);   // never borrowed
        bookRepository.flush();

        assertThat(joinRows(cleanCode.getId())).isZero();
        assertThat(authorRepository.count()).isEqualTo(2);
    }
}
