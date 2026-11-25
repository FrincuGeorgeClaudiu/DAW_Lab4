
# Library Management System - Data Model

## 1. Entities and Attributes

### Book
- id: unique identifier (Primary Key)
- title: title of the book
- isbn: unique International Standard Book Number
- published_year: year of publication

### Author
- id: unique identifier (Primary Key)
- first_name: author's first name
- last_name: author's last name

### Member
- id: unique identifier (Primary Key)
- name: member's full name
- email: unique email address
- joined_date: registration date

### Loan
- id: unique identifier (Primary Key)
- book_id: reference to Book (Foreign Key)
- member_id: reference to Member (Foreign Key)
- loan_date: date when the book was borrowed
- due_date: expected return date
- return_date: actual return date (nullable)

## 2. Natural and Surrogate Keys

| Entity | Primary Key | Natural Key |
|---|---|---|
| Book | id | isbn |
| Author | id | None |
| Member | id | email |
| Loan | id | None |

The ISBN is a natural key because it uniquely identifies a book.

The email address is a natural key because it uniquely identifies a member within the library system.

Author names are not necessarily unique.

A loan requires its own identifier because the same member may borrow the same book multiple times.

A surrogate key (id) is added to each entity to provide a stable and simple identifier that is independent of business data.

## 3. Relationships and Cardinalities

### Author - Book (Many-to-Many)

- One author can write many books.
- One book is written by one or more authors.

### Book - Loan (One-to-Many)

- One book can appear in many loans.
- Each loan refers to exactly one book.

### Member - Loan (One-to-Many)

- One member can have many loans.
- Each loan belongs to exactly one member.

### Member - Book (Many-to-Many)

- One member can borrow many books.
- One book can be borrowed by many members over time.
- This relationship is represented by the Loan entity.

## 4. Participation

| Entity | Relationship | Participation |
|---|---|---|
| Book | Author | Mandatory (1..N) |
| Author | Book | Optional (0..N) |
| Book | Loan | Optional (0..N) |
| Member | Loan | Optional (0..N) |
| Loan | Book | Mandatory (1..1) |
| Loan | Member | Mandatory (1..1) |

Every book must have at least one author.

An author may exist without a registered book.

A book may exist without being borrowed.

A member may exist without having any loans.

Every loan must reference exactly one book and one member.

## 5. Checkpoint

### The relationship between Book and Member is N-M, but it carries data (three dates). Where does that data belong, and what does that turn the relationship into?

The loan_date, due_date and return_date attributes belong to the relationship between Book and Member.

Because this relationship contains its own attributes, it becomes an associative entity called Loan.

The original many-to-many relationship is replaced by two one-to-many relationships:

- Book (1) - Loan (N)
- Member (1) - Loan (N)

The Loan entity has its own surrogate primary key, allowing the same member to borrow the same book multiple times.


## 6. Normalization

### Original Table

| member | member_email | isbn | title | publisher | publisher_city | authors | author_country | loan_date |
|---|---|---|---|---|---|---|---|---|
| Ana | ana@x.ro | 978-0132350884 | Clean Code | Prentice Hall | Boston | R. Martin | USA | 2026-10-01 |
| Ana | ana@x.ro | 978-0201485677 | Refactoring | Addison-Wesley | Boston | M. Fowler, K. Beck | UK, USA | 2026-10-03 |
| Dan | dan@x.ro | 978-0132350884 | Clean Code | Prentice Hall | Boston | R. Martin | USA | 2026-10-05 |

### 6.1. Data Anomalies

#### Update Anomaly

If the title of Clean Code changes, both the first and third
rows must be updated. Updating only one row would create
inconsistent data for the same ISBN.

#### Insert Anomaly

A new book cannot be registered independently without
providing loan and member information.

#### Delete Anomaly

If Ana's Refactoring loan is deleted, all information about
Refactoring and its authors is lost because that book appears
only in the second row.

### 6.2. First Normal Form (1NF)

The authors and author_country columns violate 1NF because
they contain multiple values in a single cell.

The second row must be divided into two separate rows.

| member | member_email | isbn | title | publisher | publisher_city | author | author_country | loan_date |
|---|---|---|---|---|---|---|---|---|
| Ana | ana@x.ro | 978-0201485677 | Refactoring | Addison-Wesley | Boston | M. Fowler | UK | 2026-10-03 |
| Ana | ana@x.ro | 978-0201485677 | Refactoring | Addison-Wesley | Boston | K. Beck | USA | 2026-10-03 |

The second row becomes two rows, so the original table
increases from three to four rows.

Each cell now contains a single atomic value.

The new composite key is:

(member_email, isbn, loan_date, author)

### 6.3. Second Normal Form (2NF)

The following functional dependencies can be identified:

- member_email -> member
- isbn -> title
- isbn -> publisher
- isbn -> publisher_city
- author -> author_country
- publisher -> publisher_city

The first five dependencies are partial dependencies
because their determinants are proper subsets of the
composite key.

To eliminate partial dependencies, the table is decomposed
into the following tables:

| Table | Columns | Primary Key |
|---|---|---|
| Member | member_email, member | member_email |
| Book | isbn, title, publisher, publisher_city | isbn |
| Author | author, author_country | author |
| Book_Author | isbn, author | isbn, author |
| Loan | member_email, isbn, loan_date | member_email, isbn, loan_date |

The Book table still contains a transitive dependency:

isbn -> publisher -> publisher_city

The publisher_city attribute depends on the publisher,
which depends on the ISBN.

### 6.4. Third Normal Form (3NF)

To eliminate the transitive dependency, a separate Publisher
table is created.

The final decomposition, using surrogate identifiers for
the main entities, is:

| Table | Columns | Primary Key |
|---|---|---|
| Publisher | id, name, city | id |
| Book | id, isbn, title, publisher_id | id |
| Author | id, name, country | id |
| Member | id, name, email | id |
| Book_Author | book_id, author_id | book_id, author_id |
| Loan | id, member_id, book_id, loan_date | id |

Natural unique keys:

- Book: isbn
- Member: email
- Publisher: name (assumed unique in this example)

Foreign keys:

- Book.publisher_id -> Publisher.id
- Book_Author.book_id -> Book.id
- Book_Author.author_id -> Author.id
- Loan.member_id -> Member.id
- Loan.book_id -> Book.id

The resulting tables satisfy 3NF because non-key attributes
depend on the key without partial or transitive dependencies.

### 6.5. Comparison with the ER Diagram

The normalized structure corresponds to the entities
identified in the ER diagram:

- Book
- Author
- Member
- Loan

The Book_Author table implements the many-to-many
relationship between Book and Author.

The normalization exercise also introduces a Publisher
table and the author's country attribute because the
original spreadsheet contains additional information
that is not required by the library application.

These additional attributes and the Publisher table
are not included in the application's required SQL schema.

### 6.6. Optional Checkpoint - Denormalization

Adding a loan_count column to the Author table would
introduce redundant derived data.

The number of loans can be calculated using the Loan
and Book_Author tables.

A stored counter would need to be updated whenever a
loan is inserted, deleted or modified, or whenever a
book's author relationships change.

For a monthly report, the calculation must also filter
loans by their loan_date.

Denormalization may be acceptable when performance
requirements justify it and a reliable mechanism
keeps the stored values consistent.


## 7. Relational Schema - SQL Checkpoints

### 7.1. Foreign Key Enforcement

With PRAGMA foreign_keys = ON, inserting a loan
that references a non-existing book fails with
a FOREIGN KEY constraint error.

Without enabling foreign keys in a new SQLite
connection, the invalid insertion succeeds.

Foreign key enforcement is configured separately
for each SQLite connection.

The invalid row was removed after the test.

### 7.2. Composite Primary Key vs Surrogate Key

The book_author table uses a composite primary
key (book_id, author_id) because each association
between a book and an author must be unique.

The loan table uses a surrogate primary key (id)
because the same member can borrow the same book
multiple times.

Using (book_id, member_id) as the primary key
would prevent repeated loans between the same
member and book.

### 7.3. Deleting a Book with Existing Loans

With foreign keys enabled, deleting a book
referenced by an existing loan fails with a
FOREIGN KEY constraint error.

The book is also referenced by book_author.

The loan history should be preserved because
it represents historical library activity.

A possible solution is to mark the book as
inactive instead of physically deleting it
from the database.

### 7.4. Books Currently on Loan

A NULL value in return_date means that the book
has not yet been returned to the library.

The following query displays all books currently
on loan, together with the member's email:

```sql
SELECT book.title, member.email
FROM loan
JOIN book ON loan.book_id = book.id
JOIN member ON loan.member_id = member.id
WHERE loan.return_date IS NULL;
```

Expected result:

| title | email |
|---|---|
| Clean Code | ana@x.ro |

### 7.5. Constraint Validation

The composite primary key prevents duplicate
associations between books and authors.

The UNIQUE constraint on member.email prevents
multiple members from using the same email.

Both constraints were successfully tested
by attempting to insert duplicate values.


## 8. Maven Dependencies - Checkpoints

### 8.1. Hibernate ORM Version

The resolved Hibernate ORM version is 7.4.5.Final.

The hibernate-community-dialects dependency uses
the same version as hibernate-core.

### 8.2. Connection Pool

The Spring Boot Data JPA starter brings in HikariCP
version 7.0.2 through spring-boot-starter-jdbc.

HikariCP manages and reuses database connections,
reducing the overhead of opening new connections.

### 8.3. SQLite JDBC Driver

The sqlite-jdbc dependency is not part of the
JPA, Hibernate and Spring Data JPA abstraction layers.

It is a database-specific JDBC driver that allows
the Java application to communicate with SQLite.

The resolved SQLite JDBC version is 3.53.2.1.

The hibernate-community-dialects dependency provides
the SQLite dialect required by Hibernate.

### 8.4. Dependency Verification

The Maven dependency:tree command completed
successfully with the BUILD SUCCESS message.


## 9. SQLite Database Configuration - Checkpoint

The SQLite database connection was successfully
established through HikariCP.

Hibernate ORM version 7.4.5.Final was initialized
using the SQLite dialect.

The library.db file was created successfully.

The .tables command returns no tables because
there are no Java classes annotated with @Entity.

The ddl-auto=update setting only creates or updates
tables based on mapped JPA entities.

Since no entities have been defined yet, Hibernate
has no table mappings to process.


## 10. Book Entity - Checkpoints

### 10.1. Why is id a Long and not a long?

Long is an object type that can have a null value,
while long is a primitive type with a default value of 0.

Before the first save, the id is null because the
entity has not yet been persisted.

The database generates the id using
GenerationType.IDENTITY.

### 10.2. Who converts publishedYear into published_year?

Hibernate uses the physical naming strategy configured
by Spring Boot to convert camelCase Java field names
into snake_case database column names.

Therefore, publishedYear becomes published_year
without an explicit @Column annotation.

### 10.3. Why must the entity not be final?

Hibernate can create proxy subclasses of entities
to support features such as lazy loading.

A final class cannot be extended.

JPA also requires a no-argument constructor to
instantiate entities when loading data from the
database, even if the developer never calls it directly.

### Checkpoint 4 - Exception Translation

When inserting a duplicate ISBN, SQLite rejects the
operation because of the UNIQUE constraint.

The observed exception is:

org.springframework.orm.jpa.JpaSystemException

JpaSystemException is a subclass of DataAccessException.

Spring translates persistence exceptions into its own
data access exception hierarchy.

Using DataAccessException in the test makes it less
dependent on a specific persistence provider or
database implementation.

The test also verifies that the exception message
contains "UNIQUE constraint failed: book.isbn".

The test completed successfully.

### Checkpoint 5 - Save vs SaveAndFlush

Replacing saveAndFlush() with save() in the duplicate
ISBN test still produced a successful test result.

The UNIQUE constraint violation was detected immediately.

This happens because the Book entity uses
GenerationType.IDENTITY, which requires Hibernate
to execute the INSERT statement to obtain the
generated identifier.

In general, save() does not guarantee an immediate
flush, while saveAndFlush() explicitly synchronizes
the persistence context with the database.

Neither method necessarily commits the transaction.

The experiment completed with BUILD SUCCESS.

### Checkpoint 6 - Invalid Derived Query

A temporary method was added to BookRepository:

List<Book> findByPrice(Double price);

The Book entity does not contain a price attribute.

Spring Data JPA could not initialize the repository
because the derived query references a nonexistent
entity property.

As a result, the ApplicationContext failed to load.

Test results:
- Tests run: 7
- Failures: 0
- Errors: 7
- Skipped: 0
- BUILD FAILURE

This demonstrates that derived query methods are
validated during repository initialization, rather
than only when the methods are called.