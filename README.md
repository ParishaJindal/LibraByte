# LibraByte – Library Queue Optimizer
> **A High-Performance Library Management and Fair FIFO Reservation System built with Pure Core Java, JDBC, and Modern Vanilla Web Technologies.**

[![Core Java](https://img.shields.io/badge/Language-Core%20Java%20(JDK%2017%2B)-007396?logo=openjdk&logoColor=white)](#)
[![Database](https://img.shields.io/badge/Database-SQLite%203%20via%20JDBC-003B57?logo=sqlite&logoColor=white)](#)
[![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20CSS3%20%7C%20Vanilla%20JS-E34F26?logo=html5&logoColor=white)](#)
[![Zero Frameworks](https://img.shields.io/badge/Frameworks-None%20(Pure%20Core%20Java)-success)](#)

---

## 1. Project Overview & Objective

**LibraByte** is a comprehensive, full-stack library management and circulation optimization platform designed to solve resource contention and book waitlist bottlenecks in modern academic and public libraries.

When high-demand books run out of physical copies, traditional libraries often suffer from unfair hold hoarding, manual clipboard tracking, or arbitrary reallocation. **LibraByte** solves this by implementing an automated **First-In-First-Out (FIFO) Reservation Queue Engine** in pure Core Java. 

### Key Highlights:
- **Zero Framework Backend**: Strictly built using Core Java concepts, `com.sun.net.httpserver.HttpServer`, standard `java.util.Collections`, and JDBC. No Spring Boot, Spring MVC, Hibernate, or third-party ORMs were used.
- **Fair FIFO Queue Allocation**: Guarantees that the first patron who reserves an out-of-stock book receives the next copy upon return. Allocated copies are reserved exclusively for that patron and kept off the public shelf.
- **Transactional Consistency**: Atomic operations using JDBC transactions (`conn.setAutoCommit(false)`, `commit()`, `rollback()`).
- **Crash & Restart Persistence**: The in-memory FIFO queue states automatically restore from SQLite upon server boot, ensuring zero queue loss.
- **Heritage Library Editorial Aesthetic**: Built with a dignified, scholarly visual system using **Warm Ivory (`#F5F1E8`)** for the canvas, **Forest Green (`#244638`)** for the sidebar navigation and primary action buttons, **Pure White (`#FFFFFF`)** for reading cards, **Muted Bronze (`#B18A58`)** for accents, and **Charcoal (`#343A35`)** typography with elegant serif headings (`Playfair Display`) paired with clean sans-serif body text. Avoids harsh gradients, neon widgets, or AI-generated aesthetic clichés.

---

## 2. Compulsory Core Java Concepts Implementation

| Core Java Concept | Concrete Implementation in Source Code | Location |
| :--- | :--- | :--- |
| **Object-Oriented Programming (OOP)** | **Inheritance & Abstraction**: `BaseEntity` abstract superclass containing `id` and `createdAt` inherited by `Book`, `Member`, `IssueRecord`, and `Reservation`.<br>**Encapsulation**: Private fields, strict getters/setters, domain validation in constructors.<br>**Polymorphism**: Overridden `toString()`, custom JSON reflection serialization, and `Comparable<Reservation>` implementation. | `backend/src/model/` |
| **`HashMap`** | Used for high-speed runtime lookups, entity caching, and waitlist routing:<br>• `Map<Integer, Book> bookMap` in `BookService`<br>• `Map<Integer, Member> memberMap` in `MemberService`<br>• `Map<Integer, Queue<Reservation>> bookReservationQueues` in `ReservationService` | `backend/src/service/` |
| **`Queue` & `LinkedList`** | Dedicated FIFO reservation queues implemented via `java.util.Queue<Reservation>` instantiated as `java.util.LinkedList<Reservation>`. Supports `offer()` for enqueuing patrons and `poll()` for fair allocation. | `backend/src/service/ReservationService.java` |
| **Custom Exception Handling** | Hierarchical custom exceptions extending `LibraryException` with HTTP status codes: `BookNotAvailableException`, `MemberNotFoundException`, `BookNotFoundException`, `DuplicateReservationException`, and `InvalidReturnException`. Handled via structured `try-catch-finally` blocks. | `backend/src/exceptions/` |
| **JDBC Connectivity & Transactions** | Standard JDBC with `Connection`, `PreparedStatement`, `ResultSet`, and `try-with-resources`. Multi-table operations (copy updates + issue/return/allocation) managed transactionally via `conn.setAutoCommit(false)`, `conn.commit()`, and `conn.rollback()`. | `backend/src/dao/`<br>`backend/src/database/` |
| **`Comparator`** | Compulsory sorting using `java.util.Comparator`:<br>1. Overdue records sorted by overdue days descending (`Comparator<OverdueReportItem>`)<br>2. Popular books sorted by borrowing frequency descending (`Comparator<PopularBookReportItem>`)<br>3. Queue entries sorted by reservation date and queue position (`Comparator<Reservation>`) | `backend/src/service/ReportService.java` |
| **Built-in `HttpServer`** | Pure Java REST API and zero-dependency static file server using `com.sun.net.httpserver.HttpServer`. Multithreaded execution pool (`Executors.newFixedThreadPool(10)`). | `backend/src/server/ApiServer.java` |

---

## 3. System Architecture & Folder Structure

```
LibraryQueueOptimizer/
├── frontend/                               # Modern Executive Dashboard
│   ├── index.html                          # Executive Dashboard with live circulation KPIs
│   ├── books.html                          # Book catalog, copy counts & CRUD operations
│   ├── members.html                        # Member directory, profiles & registration
│   ├── issue-return.html                   # Circulation desk: issue, return & queue handoff
│   ├── reservations.html                   # FIFO Reservation Queue visualizer & management
│   ├── reports.html                        # Overdue, popularity & waitlist reports with CSV export
│   ├── css/
│   │   └── style.css                       # Heritage Library: Ivory, Forest Green, Bronze & Charcoal
│   └── js/
│       ├── app.js                          # Core client, API fetcher, modals & toast alerts
│       ├── books.js                        # Book catalog search & modal handlers
│       ├── members.js                      # Member management & profile loader
│       ├── issue-return.js                 # Issue/return workflows & queue handoff alerts
│       ├── reservations.js                 # Real-time FIFO queue rendering & cancellation
│       └── reports.js                      # Comparator reports, table sorting & CSV generation
│
├── backend/
│   ├── src/
│   │   ├── Main.java                       # Main application entry point & service bootstrap
│   │   ├── TestScenarios.java              # 12-scenario automated evaluation test suite
│   │   ├── server/
│   │   │   ├── ApiServer.java              # com.sun.net.httpserver.HttpServer & static router
│   │   │   └── HttpUtils.java              # Query parser, CORS handler, JSON response sender
│   │   ├── model/
│   │   │   ├── BaseEntity.java             # Abstract OOP base class
│   │   │   ├── Book.java                   # Book domain model
│   │   │   ├── Member.java                 # Member domain model
│   │   │   ├── IssueRecord.java            # Circulation issue record
│   │   │   ├── Reservation.java            # FIFO Reservation domain model
│   │   │   ├── OverdueReportItem.java      # Overdue report DTO
│   │   │   ├── PopularBookReportItem.java  # Borrowing frequency report DTO
│   │   │   └── DashboardStats.java         # Executive dashboard stats DTO
│   │   ├── service/
│   │   │   ├── BookService.java            # HashMap-cached book service
│   │   │   ├── MemberService.java          # HashMap-cached member service
│   │   │   ├── ReservationService.java     # Queue<Reservation> FIFO engine
│   │   │   ├── LibraryService.java         # JDBC Transactional issue & return workflows
│   │   │   └── ReportService.java          # Comparator-driven analytics service
│   │   ├── dao/
│   │   │   ├── BookDao.java                # PreparedStatement JDBC queries for books
│   │   │   ├── MemberDao.java              # PreparedStatement JDBC queries for members
│   │   │   ├── IssueDao.java               # PreparedStatement JDBC queries for issues
│   │   │   └── ReservationDao.java         # PreparedStatement JDBC queries for reservations
│   │   ├── database/
│   │   │   ├── DatabaseConnection.java     # SQLite connection provider (PRAGMA foreign_keys = ON)
│   │   │   └── DatabaseInitializer.java    # Schema migration & realistic demo seeder
│   │   ├── exceptions/
│   │   │   ├── LibraryException.java       # Root library exception
│   │   │   ├── BookNotAvailableException.java # 409 Conflict when copies = 0
│   │   │   ├── BookNotFoundException.java     # 404 Not Found
│   │   │   ├── MemberNotFoundException.java   # 404 Not Found
│   │   │   ├── DuplicateReservationException.java # 409 Conflict on re-reserving
│   │   │   └── InvalidReturnException.java    # 400 Bad Request on illegal returns
│   │   └── util/
│   │       ├── JsonUtils.java              # Zero-dependency reflection JSON parser & serializer
│   │       └── DateUtils.java              # ISO date math & overdue calculator
│   ├── lib/
│   │   └── sqlite-jdbc-3.48.0.0.jar        # Official SQLite JDBC driver
│   └── database/
│       ├── schema.sql                      # DDL schema definition
│       └── librabyte.db                    # Persistent SQLite database
│
├── compile.sh                              # Single-command compilation script
├── run.sh                                  # Application launch script
├── test.sh                                 # Automated 12-scenario test harness
├── .gitignore                              # Git exclusion rules
└── README.md                               # Comprehensive documentation
```

---

## 4. Database Schema Design

The SQLite database (`backend/database/librabyte.db`) uses relational integrity with enabled foreign keys (`PRAGMA foreign_keys = ON`):

```sql
-- 1. Members
CREATE TABLE members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT NOT NULL,
    registration_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')),
    created_at TEXT NOT NULL
);

-- 2. Books
CREATE TABLE books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    isbn TEXT UNIQUE NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    category TEXT NOT NULL,
    publisher TEXT NOT NULL,
    total_copies INTEGER NOT NULL CHECK(total_copies >= 0),
    available_copies INTEGER NOT NULL CHECK(available_copies >= 0 AND available_copies <= total_copies),
    created_at TEXT NOT NULL
);

-- 3. Issue Records
CREATE TABLE issue_records (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    issue_date TEXT NOT NULL,
    due_date TEXT NOT NULL,
    return_date TEXT,
    status TEXT NOT NULL CHECK(status IN ('ISSUED', 'RETURNED', 'OVERDUE')),
    created_at TEXT NOT NULL
);

-- 4. Reservations (Persistent FIFO Queues)
CREATE TABLE reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER NOT NULL REFERENCES books(id) ON DELETE RESTRICT,
    member_id INTEGER NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    reservation_date TEXT NOT NULL,
    queue_position INTEGER NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('WAITING', 'ALLOCATED', 'CANCELLED', 'FULFILLED')),
    created_at TEXT NOT NULL
);
```

---

## 5. REST API Specifications

All endpoints run on `http://localhost:8080/api`:

| Method | Endpoint | Description | Request Body / Query |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/dashboard` | Returns total members, books, copies, active loans, waitlists, recent issues/returns, and popular books. | None |
| `GET` | `/api/books` | Returns all books or filters by query (`?search=...`). | Query param: `search` (optional) |
| `POST` | `/api/books` | Adds a new book to the library catalog. | `{ "isbn", "title", "author", "category", "publisher", "totalCopies" }` |
| `GET` | `/api/books/{id}` | Fetches individual book details by ID. | Path variable: `id` |
| `PUT` | `/api/books/{id}` | Updates existing book details. | `{ "isbn", "title", "author", "category", "publisher", "totalCopies" }` |
| `DELETE` | `/api/books/{id}` | Deletes a book (prevented if active loans/waitlists exist). | Path variable: `id` |
| `GET` | `/api/members` | Returns all members or filters by search (`?search=...`). | Query param: `search` (optional) |
| `POST` | `/api/members` | Registers a new library member. | `{ "fullName", "email", "phone", "status" }` |
| `GET` | `/api/members/{id}` | Fetches member by ID. | Path variable: `id` |
| `PUT` | `/api/members/{id}` | Updates member contact or status. | `{ "fullName", "email", "phone", "status" }` |
| `DELETE` | `/api/members/{id}` | Deletes a member (prevented if active loans/waitlists exist). | Path variable: `id` |
| `GET` | `/api/issues` | Returns all book issue records. | None |
| `POST` | `/api/issues` | Issues an available book copy (14-day duration). | `{ "bookId": 1, "memberId": 2 }` |
| `POST` | `/api/issues/return` | Returns an issued book and performs FIFO queue reallocation. | `{ "issueId": 1 }` |
| `GET` | `/api/reservations` | Lists all reservations or filters by book (`?bookId=...`). | Query param: `bookId` (optional) |
| `POST` | `/api/reservations` | Adds a member to the book's FIFO waitlist. | `{ "bookId": 1, "memberId": 2 }` |
| `DELETE` | `/api/reservations/{id}`| Cancels a reservation and shifts subsequent positions forward. | Path variable: `id` |
| `GET` | `/api/reports/overdue` | Overdue loans sorted by days overdue descending via `Comparator`. | None |
| `GET` | `/api/reports/popular-books`| Most borrowed books sorted by count descending via `Comparator`. | None |
| `GET` | `/api/reports/reservations` | All waitlist entries sorted by date & queue position via `Comparator`. | None |

---

## 6. How to Build & Run

### Prerequisites
- JDK 17, 21, or newer (`javac` and `java` available on PATH).
- No external application servers or package managers required.

### 1. Quick Start via Provided Shell Scripts

Make the execution scripts executable (one-time):
```bash
chmod +x compile.sh run.sh test.sh
```

**Step 1: Compile the Backend**
```bash
./compile.sh
```

**Step 2: Start the Web Application**
```bash
./run.sh
```

**Step 3: Open in Browser**
Navigate to:
```
http://localhost:8080/
```
The application serves both the backend REST API and the interactive frontend directly through Core Java's built-in `HttpServer`.

---

## 7. Automated Test Suite (12 Scenarios Verification)

To execute the automated test harness covering all 12 college project demonstration scenarios:
```bash
./test.sh
```

### Verification Output:
```text
=================================================================
        LIBRABYTE - COMPREHENSIVE TEST SUITE                     
=================================================================

LibraByte database initialized successfully.
Loaded reservation queues for 7 books.
[TEST 1/12] Register a new member: PASSED (Member ID: 7)
[TEST 2/12] Add a new book: PASSED (Book ID: 8, Copies: 1)
[TEST 3/12] Search books by title, author, category: PASSED (Found by title: 1, author: 2, category: 1)
[TEST 4/12] Issue an available book: PASSED (Loan #14 created, available copies reduced to 0)
[TEST 5/12] Attempt to issue an unavailable book (expect BookNotAvailableException): PASSED (Caught expected exception)
[TEST 6/12] Reserve an unavailable book: PASSED (Reservation placed at FIFO Position #1)
[TEST 7/12] Add multiple members to same reservation queue: PASSED (FIFO Queue has 3 members: Pos #1, Pos #2, Pos #3)
[TEST 8/12] Return book and verify FIFO reservation allocation: PASSED (Copy allocated to Pos #1, public available copies remained at 0, remaining queue shifted)
[TEST 9/12] Cancel a reservation and verify queue recomputation: PASSED (Reservation cancelled. Subsequent patron promoted from Pos #2 to Pos #1)
[TEST 10/12] Generate overdue report (sorted by overdue days via Comparator): PASSED (Found 2 records, correctly sorted descending)
[TEST 11/12] Generate popular books report (sorted by borrow count via Comparator): PASSED (Ranked by borrowing count via Comparator)
[TEST 12/12] Restart application simulation & verify SQLite persistence: PASSED (Queue state completely restored from SQLite after restart!)

=================================================================
   ALL 12/12 TEST SCENARIOS PASSED WITH 100% SUCCESS!   
=================================================================
```

---

## 8. Interactive Frontend Walkthrough

1. **Dashboard (`index.html`)**:
   - Executive statistics cards showing total members, cataloged titles, available copies, circulating loans, active waitlists, and overdue books.
   - Live tables for recent issues, recent returns, and popular books.
2. **Book Catalog (`books.html`)**:
   - Search books instantly across title, author, category, or ISBN.
   - Filter by category with dynamic dropdown.
   - Modal forms for adding and updating book titles.
   - Deletion validation protecting books with active loans or queues.
3. **Member Directory (`members.html`)**:
   - Register patrons with real-time email regex and phone validation.
   - Comprehensive patron profile modal displaying active loans and pending waitlists.
   - Safe deletion rules preventing removal of members with outstanding transactions.
4. **Issue & Return Desk (`issue-return.html`)**:
   - Real-time stock status indicator: green alert for available copies, red alert for 0 copies.
   - Automatically provides a one-click "Reserve in Queue" button when copies run out.
   - Returning a book triggers automatic FIFO queue allocation with distinct gold notification.
5. **Reservation Queue (`reservations.html`)**:
   - Visual queue boards with gold badges showing "#1 Next in Line", "#2 Second", etc.
   - Filter queues by specific book title.
   - One-click cancellation with automatic forward promotion of subsequent queue positions.
6. **Reports & Analytics (`reports.html`)**:
   - Tabbed view for Overdue Books, Popular Books, and Queue Waitlists.
   - Multi-column frontend sorting and real-time search.
   - One-click **CSV Export** and **Print Report** functionality.

---

## 9. Academic Evaluation Checklist

- [x] **Zero Java Frameworks**: 100% Core Java and standard JDBC.
- [x] **FIFO Reservation Queue**: Built with `Queue<Reservation>` and `LinkedList<Reservation>`.
- [x] **HashMap Caching**: In-memory caching in `BookService`, `MemberService`, and `ReservationService`.
- [x] **Custom Exceptions**: Complete hierarchy with HTTP code mapping.
- [x] **JDBC Transactions**: `setAutoCommit(false)`, `commit()`, and `rollback()` handling.
- [x] **Comparator Sorting**: Report service sorts overdue days, borrowing counts, and dates.
- [x] **Database Persistence**: Automatic queue restoration from SQLite on restart.
- [x] **Responsive Heritage Library UI**: Hand-crafted CSS with Warm Ivory, Forest Green, Bronze, and Charcoal typography without external JS frameworks.
- [x] **Automated Test Suite**: 12/12 verification test suite included.
