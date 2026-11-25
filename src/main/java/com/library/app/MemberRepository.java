package com.library.app;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("select distinct m from Member m left join fetch m.loans")
    List<Member> findAllWithLoans();

    @EntityGraph(attributePaths = "loans")
    List<Member> findAllByOrderByNameAsc();
}