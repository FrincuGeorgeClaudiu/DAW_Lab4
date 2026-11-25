package com.library.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FetchingTest {

    @Autowired BookRepository bookRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired LoanRepository loanRepository;
    @Autowired EntityManager em;

    Statistics stats;

    @BeforeEach
    void threeMembersWithOneLoanAndOneWithout() {
        Book book = bookRepository.save(new Book("Clean Code", "978-0132350884"));
        for (String name : List.of("Ana", "Dan", "Eva")) {
            Member member = memberRepository.save(
                    new Member(name, name.toLowerCase() + "@x.ro", LocalDate.of(2026, 9, 1)));
            Loan loan = new Loan(book, member,
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
            member.addLoan(loan);
            loanRepository.save(loan);
        }
        memberRepository.save(
                new Member("Fay", "fay@x.ro", LocalDate.of(2026, 9, 1)));   // no loans
        em.flush();
        em.clear();   // nothing is cached: everything below hits the database

        stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    /** Runs the query, touches the loans of every member, counts the statements. */
    long statementsFor(Supplier<List<Member>> query) {
        stats.clear();
        for (Member m : query.get()) {
            m.getLoans().size();
        }
        return stats.getPrepareStatementCount();
    }

    @Test
    void lazyLoadingIsOnePlusN() {
        assertThat(statementsFor(memberRepository::findAll)).isEqualTo(1 + 4);
    }

    @Test
    void joinFetchNeedsOneStatement() {
        assertThat(statementsFor(memberRepository::findAllWithLoans)).isEqualTo(1);
    }

    @Test
    void joinFetchKeepsMembersWithoutLoans() {
        assertThat(memberRepository.findAllWithLoans()).hasSize(4);
    }

    @Test
    void entityGraphNeedsOneStatement() {
        assertThat(statementsFor(memberRepository::findAllByOrderByNameAsc)).isEqualTo(1);
    }
}
