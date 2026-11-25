
package com.library.app;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    @EntityGraph(attributePaths = "book")
    List<Loan> findByMemberIdAndReturnDateIsNull(Long memberId);

    long countByMemberIdAndReturnDateIsNull(Long memberId);
}