package com.library.app;

public record BookDto(
        Long id,
        String title,
        String isbn,
        Integer publishedYear
) {
    public static BookDto from(Book book) {
        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getPublishedYear()
        );
    }
}