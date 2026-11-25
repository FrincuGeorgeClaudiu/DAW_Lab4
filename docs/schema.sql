
PRAGMA foreign_keys = ON;

CREATE TABLE author (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        first_name TEXT NOT NULL,
                        last_name TEXT NOT NULL
);

CREATE TABLE book (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      title TEXT NOT NULL,
                      isbn TEXT NOT NULL UNIQUE,
                      published_year INTEGER
);

CREATE TABLE book_author (
                             book_id INTEGER NOT NULL REFERENCES book(id),
                             author_id INTEGER NOT NULL REFERENCES author(id),
                             PRIMARY KEY (book_id, author_id)
);

CREATE TABLE member (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL UNIQUE,
                        joined_at TEXT NOT NULL
);

CREATE TABLE loan (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      book_id INTEGER NOT NULL REFERENCES book(id),
                      member_id INTEGER NOT NULL REFERENCES member(id),
                      loan_date TEXT NOT NULL,
                      due_date TEXT NOT NULL,
                      return_date TEXT
);

-- Insert authors

INSERT INTO author (first_name, last_name)
VALUES ('Robert', 'Martin');

INSERT INTO author (first_name, last_name)
VALUES ('Martin', 'Fowler');

-- Insert books

INSERT INTO book (title, isbn, published_year)
VALUES ('Clean Code', '978-0132350884', 2008);

INSERT INTO book (title, isbn, published_year)
VALUES ('Refactoring', '978-0201485677', 1999);

-- Associate books with authors

INSERT INTO book_author (book_id, author_id)
VALUES (1, 1);

INSERT INTO book_author (book_id, author_id)
VALUES (2, 1);

INSERT INTO book_author (book_id, author_id)
VALUES (2, 2);

-- Insert member

INSERT INTO member (name, email, joined_at)
VALUES ('Ana', 'ana@x.ro', '2026-10-01');

-- Insert loan

INSERT INTO loan (book_id, member_id, loan_date, due_date)
VALUES (1, 1, '2026-10-29', '2026-11-12');
