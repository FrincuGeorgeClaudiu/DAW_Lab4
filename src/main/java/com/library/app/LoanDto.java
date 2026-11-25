package com.library.app;

import java.time.LocalDate;

public record LoanDto(
        Long id,
        Long bookId,
        String bookTitle,
        LocalDate loanDate,
        LocalDate dueDate
) {
    public static LoanDto from(Loan loan) {
        return new LoanDto(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getLoanDate(),
                loan.getDueDate()
        );
    }
}