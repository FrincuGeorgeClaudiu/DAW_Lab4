
package com.library.app;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;

    @OneToMany(mappedBy = "member")
    private List<Loan> loans = new ArrayList<>();

    protected Member() {
    }

    public Member(String name, String email,
                  LocalDate joinedAt) {
        this.name = name;
        this.email = email;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getJoinedAt() {
        return joinedAt;
    }

    public List<Loan> getLoans() {
        return loans;
    }

    public void addLoan(Loan loan) {
        loans.add(loan);
        loan.setMember(this);
    }
}
