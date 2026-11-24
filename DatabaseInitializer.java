package database;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import util.DateUtils;
import util.PasswordUtils;

/**
 * Database schema and initial seed data initializer for LibraByte.
 * Populates realistic Computer Science & Engineering (CSE) academic textbooks across 7 categories.
 * Seeds a diverse and realistic roster of 20 Indian university student members, loan records, and FIFO reservation queues.
 * Ensures idempotent migrations preserving existing user accounts (Admin & real users) and their transaction history.
 */
public class DatabaseInitializer {

    // 20 Realistic Indian University Student Members across various CSE batches
    public static final Object[][] INDIAN_MEMBERS = {
            {"Aarav Sharma", "aarav.sharma@gmail.com", "+91-98101-23401", "2026-07-15", "ACTIVE"},
            {"Ananya Verma", "ananya.verma@gmail.com", "+91-98102-34502", "2026-07-20", "ACTIVE"},
            {"Rohan Mehta", "rohan.mehta@gmail.com", "+91-98103-45603", "2026-08-05", "ACTIVE"},
            {"Priya Malhotra", "priya.malhotra@gmail.com", "+91-98104-56704", "2026-08-12", "ACTIVE"},
            {"Arjun Kapoor", "arjun.kapoor@gmail.com", "+91-98105-67805", "2026-08-25", "ACTIVE"},
            {"Simran Kaur", "simran.kaur@gmail.com", "+91-98106-78906", "2026-09-02", "ACTIVE"},
            {"Aditya Singh", "aditya.singh@gmail.com", "+91-98107-89007", "2026-09-10", "ACTIVE"},
            {"Ishita Gupta", "ishita.gupta@gmail.com", "+91-98108-90108", "2026-09-18", "ACTIVE"},
            {"Kabir Khanna", "kabir.khanna@gmail.com", "+91-98109-01209", "2026-10-01", "ACTIVE"},
            {"Mehak Arora", "mehak.arora@gmail.com", "+91-98110-12310", "2026-10-08", "ACTIVE"},
            {"Rahul Bansal", "rahul.bansal@gmail.com", "+91-98111-23411", "2026-10-15", "ACTIVE"},
            {"Aditi Jain", "aditi.jain@gmail.com", "+91-98112-34512", "2026-10-22", "ACTIVE"},
            {"Harsh Vardhan", "harsh.vardhan@gmail.com", "+91-98113-45613", "2026-11-01", "ACTIVE"},
            {"Navya Sood", "navya.sood@gmail.com", "+91-98114-56714", "2026-11-05", "ACTIVE"},
            {"Karan Malhotra", "karan.malhotra@gmail.com", "+91-98115-67815", "2026-11-10", "ACTIVE"},
            {"Riya Chawla", "riya.chawla@gmail.com", "+91-98116-78916", "2026-11-12", "ACTIVE"},
            {"Manav Aggarwal", "manav.aggarwal@gmail.com", "+91-98117-89017", "2026-11-14", "ACTIVE"},
            {"Jasleen Kaur", "jasleen.kaur@gmail.com", "+91-98118-90118", "2026-11-16", "ACTIVE"},
            {"Devansh Mittal", "devansh.mittal@gmail.com", "+91-98119-01219", "2026-11-18", "ACTIVE"},
            {"Tanvi Sharma", "tanvi.sharma@gmail.com", "+91-98120-12320", "2026-11-20", "ACTIVE"}
    };

    // 28 Standard Computer Science & Engineering Textbooks across 7 Categories
    public static final Object[][] CSE_BOOKS = {
            // 1. C Programming
            {"978-0131103627", "The C Programming Language", "Brian W. Kernighan and Dennis M. Ritchie", "C Programming", "Prentice Hall", 4, 0},
            {"978-9353165130", "Programming in ANSI C", "E. Balagurusamy", "C Programming", "McGraw-Hill", 3, 1},
            {"978-9387284494", "Let Us C", "Yashavant Kanetkar", "C Programming", "BPB Publications", 5, 5},
            {"978-0070411838", "C: The Complete Reference", "Herbert Schildt", "C Programming", "McGraw-Hill", 3, 3},

            // 2. Computer Networks
            {"978-0133594140", "Computer Networking: A Top-Down Approach", "James Kurose and Keith Ross", "Computer Networks", "Pearson", 4, 2},
            {"978-0132126953", "Computer Networks", "Andrew S. Tanenbaum", "Computer Networks", "Pearson", 3, 3},
            {"978-0073376226", "Data Communications and Networking", "Behrouz A. Forouzan", "Computer Networks", "McGraw-Hill", 4, 4},
            {"978-0123850591", "Computer Networks: A Systems Approach", "Larry Peterson and Bruce Davie", "Computer Networks", "Morgan Kaufmann", 3, 3},

            // 3. Operating Systems
            {"978-1118063330", "Operating System Concepts", "Abraham Silberschatz, Peter B. Galvin and Greg Gagne", "Operating Systems", "Wiley", 4, 0},
            {"978-0133591620", "Modern Operating Systems", "Andrew S. Tanenbaum and Herbert Bos", "Operating Systems", "Pearson", 3, 3},
            {"978-1985086593", "Operating Systems: Three Easy Pieces", "Remzi H. Arpaci-Dusseau and Andrea C. Arpaci-Dusseau", "Operating Systems", "CreateSpace", 4, 4},
            {"978-0134670959", "Operating Systems", "William Stallings", "Operating Systems", "Pearson", 3, 3},

            // 4. Java Programming
            {"978-0134685991", "Effective Java", "Joshua Bloch", "Java Programming", "Addison-Wesley", 4, 3},
            {"978-0596009205", "Head First Java", "Kathy Sierra and Bert Bates", "Java Programming", "O'Reilly Media", 4, 4},
            {"978-1260440232", "Java: The Complete Reference", "Herbert Schildt", "Java Programming", "McGraw-Hill", 5, 5},
            {"978-0135115701", "Core Java Volume I", "Cay S. Horstmann", "Java Programming", "Prentice Hall", 3, 3},

            // 5. Database Management Systems (DBMS)
            {"978-0078022159", "Database System Concepts", "Abraham Silberschatz, Henry F. Korth and S. Sudarshan", "Database Management Systems (DBMS)", "McGraw-Hill", 4, 4},
            {"978-0133970777", "Fundamentals of Database Systems", "Ramez Elmasri and Shamkant B. Navathe", "Database Management Systems (DBMS)", "Pearson", 3, 3},
            {"978-0072465631", "Database Management Systems", "Raghu Ramakrishnan and Johannes Gehrke", "Database Management Systems (DBMS)", "McGraw-Hill", 3, 0},
            {"978-0071592550", "SQL: The Complete Reference", "James R. Groff and Paul N. Weinberg", "Database Management Systems (DBMS)", "McGraw-Hill", 4, 4},

            // 6. Frontend Development
            {"978-1118008188", "HTML and CSS: Design and Build Websites", "Jon Duckett", "Frontend Development", "Wiley", 4, 4},
            {"978-1118531648", "JavaScript and JQuery", "Jon Duckett", "Frontend Development", "Wiley", 4, 4},
            {"978-1593279509", "Eloquent JavaScript", "Marijn Haverbeke", "Frontend Development", "No Starch Press", 3, 3},
            {"978-1491960202", "Learning Web Design", "Jennifer Niederst Robbins", "Frontend Development", "O'Reilly Media", 3, 3},

            // 7. Discrete Mathematics
            {"978-0073383095", "Discrete Mathematics and Its Applications", "Kenneth H. Rosen", "Discrete Mathematics", "McGraw-Hill", 5, 5},
            {"978-0132297516", "Discrete Mathematical Structures", "Bernard Kolman, Robert C. Busby and Sharon Ross", "Discrete Mathematics", "Pearson", 3, 3},
            {"978-0070381339", "Elements of Discrete Mathematics", "C. L. Liu", "Discrete Mathematics", "McGraw-Hill", 3, 3},
            {"978-0495391326", "Discrete Mathematics with Applications", "Susanna S. Epp", "Discrete Mathematics", "Cengage Learning", 4, 4}
    };

    public static void initializeDatabase() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Members Table
            stmt.execute("CREATE TABLE IF NOT EXISTS members (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "full_name TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "phone TEXT NOT NULL, " +
                    "registration_date TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE')), " +
                    "created_at TEXT NOT NULL" +
                    ");");

            // 2. Books Table
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "isbn TEXT UNIQUE NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "publisher TEXT NOT NULL, " +
                    "total_copies INTEGER NOT NULL CHECK(total_copies >= 0), " +
                    "available_copies INTEGER NOT NULL CHECK(available_copies >= 0 AND available_copies <= total_copies), " +
                    "created_at TEXT NOT NULL" +
                    ");");

            // 3. Issue Records Table
            stmt.execute("CREATE TABLE IF NOT EXISTS issue_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER NOT NULL, " +
                    "member_id INTEGER NOT NULL, " +
                    "issue_date TEXT NOT NULL, " +
                    "due_date TEXT NOT NULL, " +
                    "return_date TEXT, " +
                    "status TEXT NOT NULL CHECK(status IN ('ISSUED', 'RETURNED', 'OVERDUE')), " +
                    "created_at TEXT NOT NULL, " +
                    "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT, " +
                    "FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT" +
                    ");");

            // 4. Reservations Table
            stmt.execute("CREATE TABLE IF NOT EXISTS reservations (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER NOT NULL, " +
                    "member_id INTEGER NOT NULL, " +
                    "reservation_date TEXT NOT NULL, " +
                    "queue_position INTEGER NOT NULL, " +
                    "status TEXT NOT NULL CHECK(status IN ('WAITING', 'ALLOCATED', 'CANCELLED', 'FULFILLED')), " +
                    "created_at TEXT NOT NULL, " +
                    "FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE RESTRICT, " +
                    "FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT" +
                    ");");

            // 5. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "full_name TEXT NOT NULL, " +
                    "email TEXT UNIQUE NOT NULL, " +
                    "phone TEXT NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "salt TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK(role IN ('ADMIN', 'MEMBER')), " +
                    "member_id INTEGER, " +
                    "created_at TEXT NOT NULL, " +
                    "profile_image TEXT DEFAULT '', " +
                    "FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL" +
                    ");");

            // Migration: Add profile_image column if upgrading an existing installation
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN profile_image TEXT DEFAULT ''");
            } catch (SQLException ignored) {
                // Column already present
            }

            // Create Indexes for query optimization
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_members_email ON members(email);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_members_name ON members(full_name);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_author ON books(author);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_category ON books(category);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_issues_book_id ON issue_records(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_issues_member_id ON issue_records(member_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_book_id ON reservations(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_reservations_member_id ON reservations(member_id);");

            // 1. Seed and migrate CSE books
            seedOrMigrateCseBooks(conn);

            // 2. Seed and migrate Indian students roster
            seedOrMigrateIndianMembers(conn);

            // 3. Seed and synchronize realistic Indian loans, overdue records, and reservation queues
            seedOrMigrateIndianLoansAndQueues(conn);

            // 4. Seed and update default user credentials
            seedDefaultUsers(conn);

            System.out.println("LibraByte database initialized successfully with realistic Indian university members and CSE catalog.");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Safely migrates old sample book titles in-place without violating foreign keys,
     * and idempotently inserts any missing CSE academic textbooks.
     */
    private static void seedOrMigrateCseBooks(Connection conn) throws SQLException {
        String now = DateUtils.now();

        // 1. In-place mapping of old generic demo books to maintain existing issue/reservation links
        String[][] oldToNewMap = {
                {"978-0132350884", "978-0131103627", "The C Programming Language", "Brian W. Kernighan and Dennis M. Ritchie", "C Programming", "Prentice Hall"},
                {"978-0201633610", "978-9353165130", "Programming in ANSI C", "E. Balagurusamy", "C Programming", "McGraw-Hill"},
                {"978-0262033848", "978-1118063330", "Operating System Concepts", "Abraham Silberschatz, Peter B. Galvin and Greg Gagne", "Operating Systems", "Wiley"},
                {"978-0135957059", "978-9387284494", "Let Us C", "Yashavant Kanetkar", "C Programming", "BPB Publications"},
                {"978-1449373320", "978-0078022159", "Database System Concepts", "Abraham Silberschatz, Henry F. Korth and S. Sudarshan", "Database Management Systems (DBMS)", "McGraw-Hill"},
                {"978-0134610993", "978-0134685991", "Effective Java", "Joshua Bloch", "Java Programming", "Addison-Wesley"}
        };

        String updateSql = "UPDATE books SET isbn = ?, title = ?, author = ?, category = ?, publisher = ? WHERE isbn = ? OR title = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            for (String[] mapping : oldToNewMap) {
                pstmt.setString(1, mapping[1]);
                pstmt.setString(2, mapping[2]);
                pstmt.setString(3, mapping[3]);
                pstmt.setString(4, mapping[4]);
                pstmt.setString(5, mapping[5]);
                pstmt.setString(6, mapping[0]);
                pstmt.setString(7, mapping[2]);
                pstmt.executeUpdate();
            }
        }

        // Clean up temporary test-created book records that have no active loans or reservations
        try (Statement cleanupStmt = conn.createStatement()) {
            cleanupStmt.executeUpdate("DELETE FROM reservations WHERE book_id IN (SELECT id FROM books WHERE title LIKE 'Structure and Interpretation%')");
            cleanupStmt.executeUpdate("DELETE FROM issue_records WHERE book_id IN (SELECT id FROM books WHERE title LIKE 'Structure and Interpretation%')");
            cleanupStmt.executeUpdate("DELETE FROM books WHERE title LIKE 'Structure and Interpretation%'");
        } catch (SQLException ignored) {}

        // 2. Insert or update all 28 CSE books idempotently
        String selectSql = "SELECT id, total_copies, available_copies FROM books WHERE isbn = ? OR title = ?";
        String insertBookSql = "INSERT INTO books (isbn, title, author, category, publisher, total_copies, available_copies, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updateBookDetailsSql = "UPDATE books SET title = ?, author = ?, category = ?, publisher = ?, total_copies = ? WHERE isbn = ?";

        try (PreparedStatement selectStmt = conn.prepareStatement(selectSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertBookSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateBookDetailsSql)) {

            for (Object[] bookData : CSE_BOOKS) {
                String isbn = (String) bookData[0];
                String title = (String) bookData[1];
                String author = (String) bookData[2];
                String category = (String) bookData[3];
                String publisher = (String) bookData[4];
                int totalCopies = (Integer) bookData[5];
                int availableCopies = (Integer) bookData[6];

                selectStmt.setString(1, isbn);
                selectStmt.setString(2, title);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        updateStmt.setString(1, title);
                        updateStmt.setString(2, author);
                        updateStmt.setString(3, category);
                        updateStmt.setString(4, publisher);
                        updateStmt.setInt(5, totalCopies);
                        updateStmt.setString(6, isbn);
                        updateStmt.executeUpdate();
                    } else {
                        insertStmt.setString(1, isbn);
                        insertStmt.setString(2, title);
                        insertStmt.setString(3, author);
                        insertStmt.setString(4, category);
                        insertStmt.setString(5, publisher);
                        insertStmt.setInt(6, totalCopies);
                        insertStmt.setInt(7, availableCopies);
                        insertStmt.setString(8, now);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Seeds and migrates library members to realistic Indian names and contacts.
     * In-place migrates any old generic demo members (Alex Rivera, Marcus Vance, etc.)
     * without breaking existing foreign key relations.
     */
    private static void seedOrMigrateIndianMembers(Connection conn) throws SQLException {
        String now = DateUtils.now();

        // 1. In-place rename of early demo members to realistic Indian university students
        String[][] oldToNewMembers = {
                {"alex.rivera@example.com", "Aarav Sharma", "aarav.sharma@gmail.com", "+91-98101-23401"},
                {"priya.sharma@example.com", "Ananya Verma", "ananya.verma@gmail.com", "+91-98102-34502"},
                {"marcus.vance@example.com", "Rohan Mehta", "rohan.mehta@gmail.com", "+91-98103-45603"},
                {"sophia.chen@example.com", "Priya Malhotra", "priya.malhotra@gmail.com", "+91-98104-56704"},
                {"daniel.m@example.com", "Arjun Kapoor", "arjun.kapoor@gmail.com", "+91-98105-67805"},
                {"emily.watson@example.com", "Simran Kaur", "simran.kaur@gmail.com", "+91-98106-78906"}
        };

        String updateMemberSql = "UPDATE members SET full_name = ?, email = ?, phone = ? WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateMemberSql)) {
            for (String[] mapping : oldToNewMembers) {
                pstmt.setString(1, mapping[1]);
                pstmt.setString(2, mapping[2]);
                pstmt.setString(3, mapping[3]);
                pstmt.setString(4, mapping[0]);
                pstmt.executeUpdate();
            }
        }

        // 2. Clean up transient comic-book/fictional test members if any exist
        try (Statement cleanup = conn.createStatement()) {
            String testCondition = "email LIKE '%@dailyplanet.com' OR email LIKE '%@wayne.com' OR " +
                    "email LIKE '%@amazon.com' OR email LIKE '%@star.com' OR email LIKE '%@hogwarts.edu' OR " +
                    "email LIKE '%.test.%' OR email LIKE '%@example.com'";
            cleanup.executeUpdate("DELETE FROM reservations WHERE member_id IN (SELECT id FROM members WHERE " + testCondition + ")");
            cleanup.executeUpdate("DELETE FROM issue_records WHERE member_id IN (SELECT id FROM members WHERE " + testCondition + ")");
            cleanup.executeUpdate("DELETE FROM users WHERE " + testCondition);
            cleanup.executeUpdate("DELETE FROM members WHERE " + testCondition);
        } catch (SQLException ignored) {}

        // 3. Ensure all 20 Indian university student members are inserted idempotently
        String checkMemberSql = "SELECT id FROM members WHERE email = ? OR full_name = ?";
        String insertMemberSql = "INSERT INTO members (full_name, email, phone, registration_date, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String updateMemberInfoSql = "UPDATE members SET full_name = ?, phone = ?, registration_date = ?, status = ? WHERE email = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkMemberSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertMemberSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateMemberInfoSql)) {

            for (Object[] m : INDIAN_MEMBERS) {
                String fullName = (String) m[0];
                String email = (String) m[1];
                String phone = (String) m[2];
                String regDate = (String) m[3];
                String status = (String) m[4];

                checkStmt.setString(1, email);
                checkStmt.setString(2, fullName);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // Already exists: update phone and details to ensure consistency
                        updateStmt.setString(1, fullName);
                        updateStmt.setString(2, phone);
                        updateStmt.setString(3, regDate);
                        updateStmt.setString(4, status);
                        updateStmt.setString(5, email);
                        updateStmt.executeUpdate();
                    } else {
                        // Insert new member
                        insertStmt.setString(1, fullName);
                        insertStmt.setString(2, email);
                        insertStmt.setString(3, phone);
                        insertStmt.setString(4, regDate);
                        insertStmt.setString(5, status);
                        insertStmt.setString(6, now);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }

    /**
     * Seeds and synchronizes realistic library borrowing activity, overdue loans,
     * historical returns, and FIFO reservation queues for the Indian student roster.
     */
    private static void seedOrMigrateIndianLoansAndQueues(Connection conn) throws SQLException {
        String now = DateUtils.now();

        // 1. Cleanly clear previous sample loans and reservations while strictly preserving real user transactions
        try (Statement cleanup = conn.createStatement()) {
            cleanup.executeUpdate("DELETE FROM reservations WHERE member_id NOT IN (SELECT member_id FROM users WHERE email = 'parishajindal17@gmail.com' AND member_id IS NOT NULL)");
            cleanup.executeUpdate("DELETE FROM issue_records WHERE member_id NOT IN (SELECT member_id FROM users WHERE email = 'parishajindal17@gmail.com' AND member_id IS NOT NULL)");
        } catch (SQLException ignored) {}

        // Cache member ID mapping by email
        Map<String, Integer> memberMap = new HashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT email, id FROM members")) {
            while (rs.next()) {
                memberMap.put(rs.getString("email"), rs.getInt("id"));
            }
        }

        // Cache book ID mapping by title
        Map<String, Integer> bookMap = new HashMap<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT title, id FROM books")) {
            while (rs.next()) {
                bookMap.put(rs.getString("title"), rs.getInt("id"));
            }
        }

        // 1. Desired Active and Overdue Loans
        // Format: {bookTitle, memberEmail, issueDate, dueDate, returnDateOrNull, status}
        Object[][] loans = {
                // Book: The C Programming Language (4 copies total: all 4 issued/overdue -> 0 available)
                {"The C Programming Language", "aarav.sharma@gmail.com", "2026-11-04", "2026-11-18", null, "OVERDUE"},
                {"The C Programming Language", "rohan.mehta@gmail.com", "2026-11-19", "2026-12-03", null, "ISSUED"},
                {"The C Programming Language", "arjun.kapoor@gmail.com", "2026-11-16", "2026-11-30", null, "ISSUED"},
                {"The C Programming Language", "priya.malhotra@gmail.com", "2026-11-20", "2026-12-04", null, "ISSUED"},

                // Book: Programming in ANSI C (3 copies total: 2 issued/overdue -> 1 available)
                {"Programming in ANSI C", "ananya.verma@gmail.com", "2026-11-17", "2026-12-01", null, "ISSUED"},
                {"Programming in ANSI C", "kabir.khanna@gmail.com", "2026-11-06", "2026-11-20", null, "OVERDUE"},

                // Book: Operating System Concepts (4 copies total: all 4 issued -> 0 available)
                {"Operating System Concepts", "mehak.arora@gmail.com", "2026-11-20", "2026-12-04", null, "ISSUED"},
                {"Operating System Concepts", "rahul.bansal@gmail.com", "2026-11-14", "2026-11-28", null, "ISSUED"},
                {"Operating System Concepts", "aditi.jain@gmail.com", "2026-11-18", "2026-12-02", null, "ISSUED"},
                {"Operating System Concepts", "harsh.vardhan@gmail.com", "2026-11-15", "2026-11-29", null, "ISSUED"},

                // Book: Computer Networking: A Top-Down Approach (4 copies total: 2 issued -> 2 available)
                {"Computer Networking: A Top-Down Approach", "riya.chawla@gmail.com", "2026-11-22", "2026-12-06", null, "ISSUED"},
                {"Computer Networking: A Top-Down Approach", "manav.aggarwal@gmail.com", "2026-11-21", "2026-12-05", null, "ISSUED"},

                // Book: Database Management Systems (3 copies total: all 3 issued -> 0 available)
                {"Database Management Systems", "devansh.mittal@gmail.com", "2026-11-12", "2026-11-26", null, "ISSUED"},
                {"Database Management Systems", "tanvi.sharma@gmail.com", "2026-11-16", "2026-11-30", null, "ISSUED"},
                {"Database Management Systems", "jasleen.kaur@gmail.com", "2026-11-18", "2026-12-02", null, "ISSUED"},

                // Book: Effective Java (4 copies total: 1 issued -> 3 available)
                {"Effective Java", "aditya.singh@gmail.com", "2026-11-23", "2026-12-07", null, "ISSUED"},

                // Historical Returned Records (Establishes realistic borrow counts for Popular Books Report)
                {"The C Programming Language", "ananya.verma@gmail.com", "2026-10-10", "2026-10-24", "2026-10-24", "RETURNED"},
                {"The C Programming Language", "simran.kaur@gmail.com", "2026-10-20", "2026-11-03", "2026-11-03", "RETURNED"},
                {"The C Programming Language", "rohan.mehta@gmail.com", "2026-10-12", "2026-10-26", "2026-10-26", "RETURNED"},
                {"The C Programming Language", "aditya.singh@gmail.com", "2026-10-25", "2026-11-08", "2026-11-08", "RETURNED"},

                {"Programming in ANSI C", "aarav.sharma@gmail.com", "2026-10-15", "2026-10-29", "2026-10-29", "RETURNED"},
                {"Programming in ANSI C", "priya.malhotra@gmail.com", "2026-10-22", "2026-11-05", "2026-11-05", "RETURNED"},

                {"Operating System Concepts", "rohan.mehta@gmail.com", "2026-10-25", "2026-11-08", "2026-11-08", "RETURNED"},
                {"Operating System Concepts", "kabir.khanna@gmail.com", "2026-10-28", "2026-11-11", "2026-11-11", "RETURNED"},

                {"Let Us C", "simran.kaur@gmail.com", "2026-10-30", "2026-11-13", "2026-11-13", "RETURNED"},
                {"Database System Concepts", "arjun.kapoor@gmail.com", "2026-11-04", "2026-11-18", "2026-11-18", "RETURNED"},
                {"Effective Java", "harsh.vardhan@gmail.com", "2026-11-02", "2026-11-16", "2026-11-16", "RETURNED"}
        };

        // Idempotently insert each loan record if not already recorded
        String checkLoanSql = "SELECT id FROM issue_records WHERE book_id = ? AND member_id = ? AND issue_date = ?";
        String insertLoanSql = "INSERT INTO issue_records (book_id, member_id, issue_date, due_date, return_date, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String updateLoanSql = "UPDATE issue_records SET due_date = ?, return_date = ?, status = ? WHERE id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkLoanSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertLoanSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateLoanSql)) {

            for (Object[] loan : loans) {
                String title = (String) loan[0];
                String email = (String) loan[1];
                String issueDate = (String) loan[2];
                String dueDate = (String) loan[3];
                String returnDate = (String) loan[4];
                String status = (String) loan[5];

                Integer bookId = bookMap.get(title);
                Integer memberId = memberMap.get(email);

                if (bookId != null && memberId != null) {
                    checkStmt.setInt(1, bookId);
                    checkStmt.setInt(2, memberId);
                    checkStmt.setString(3, issueDate);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int existingId = rs.getInt("id");
                            updateStmt.setString(1, dueDate);
                            updateStmt.setString(2, returnDate);
                            updateStmt.setString(3, status);
                            updateStmt.setInt(4, existingId);
                            updateStmt.executeUpdate();
                        } else {
                            insertStmt.setInt(1, bookId);
                            insertStmt.setInt(2, memberId);
                            insertStmt.setString(3, issueDate);
                            insertStmt.setString(4, dueDate);
                            insertStmt.setString(5, returnDate);
                            insertStmt.setString(6, status);
                            insertStmt.setString(7, now);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        }

        // 2. Desired FIFO Reservation Queues
        // Format: {bookTitle, memberEmail, reservationDate, queuePosition, status}
        Object[][] reservations = {
                // Book: The C Programming Language (Unavailable, 3 students waiting in FIFO queue)
                {"The C Programming Language", "simran.kaur@gmail.com", "2026-11-21", 1, "WAITING"},
                {"The C Programming Language", "aditya.singh@gmail.com", "2026-11-22", 2, "WAITING"},
                {"The C Programming Language", "ishita.gupta@gmail.com", "2026-11-23", 3, "WAITING"},

                // Book: Operating System Concepts (Unavailable, 2 students waiting)
                {"Operating System Concepts", "navya.sood@gmail.com", "2026-11-22", 1, "WAITING"},
                {"Operating System Concepts", "karan.malhotra@gmail.com", "2026-11-23", 2, "WAITING"},

                // Book: Database Management Systems (Unavailable, 1 student waiting)
                {"Database Management Systems", "aarav.sharma@gmail.com", "2026-11-21", 1, "WAITING"}
        };

        String checkResSql = "SELECT id FROM reservations WHERE book_id = ? AND member_id = ? AND status = 'WAITING'";
        String insertResSql = "INSERT INTO reservations (book_id, member_id, reservation_date, queue_position, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String updateResSql = "UPDATE reservations SET reservation_date = ?, queue_position = ?, status = ? WHERE id = ?";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkResSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertResSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateResSql)) {

            for (Object[] r : reservations) {
                String title = (String) r[0];
                String email = (String) r[1];
                String resDate = (String) r[2];
                int position = (Integer) r[3];
                String status = (String) r[4];

                Integer bookId = bookMap.get(title);
                Integer memberId = memberMap.get(email);

                if (bookId != null && memberId != null) {
                    checkStmt.setInt(1, bookId);
                    checkStmt.setInt(2, memberId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            int existingId = rs.getInt("id");
                            updateStmt.setString(1, resDate);
                            updateStmt.setInt(2, position);
                            updateStmt.setString(3, status);
                            updateStmt.setInt(4, existingId);
                            updateStmt.executeUpdate();
                        } else {
                            insertStmt.setInt(1, bookId);
                            insertStmt.setInt(2, memberId);
                            insertStmt.setString(3, resDate);
                            insertStmt.setInt(4, position);
                            insertStmt.setString(5, status);
                            insertStmt.setString(6, now);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }
        }

        // 3. Synchronize available_copies mathematically with active loans (ISSUED / OVERDUE)
        String syncCopiesSql = "UPDATE books SET available_copies = total_copies - " +
                "(SELECT COUNT(*) FROM issue_records WHERE issue_records.book_id = books.id AND issue_records.status IN ('ISSUED', 'OVERDUE'))";
        try (Statement syncStmt = conn.createStatement()) {
            syncStmt.executeUpdate(syncCopiesSql);
        }
    }

    /**
     * Seeds and maintains default authenticated user accounts (Admin and student patron accounts).
     * Preserves real accounts (e.g. Parisha Jindal) while ensuring sample Indian student accounts are available.
     */
    private static void seedDefaultUsers(Connection conn) throws SQLException {
        String now = DateUtils.now();

        // 1. Ensure Head Librarian (Admin) exists
        String checkAdminSql = "SELECT id FROM users WHERE email = 'admin@librabyte.com'";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(checkAdminSql)) {
            if (!rs.next()) {
                String adminSalt = PasswordUtils.generateSalt();
                String adminHash = PasswordUtils.hashPassword("Admin@123", adminSalt);
                String insertAdminSql = "INSERT INTO users (full_name, email, phone, password_hash, salt, role, member_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                    pstmt.setString(1, "Head Librarian (Admin)");
                    pstmt.setString(2, "admin@librabyte.com");
                    pstmt.setString(3, "+91-98100-00000");
                    pstmt.setString(4, adminHash);
                    pstmt.setString(5, adminSalt);
                    pstmt.setString(6, "ADMIN");
                    pstmt.setNull(7, Types.INTEGER);
                    pstmt.setString(8, now);
                    pstmt.executeUpdate();
                }
            }
        }

        // 2. In-place update old sample user logins if they exist
        String[][] userMigrations = {
                {"alex.rivera@example.com", "Aarav Sharma", "aarav.sharma@gmail.com", "+91-98101-23401"},
                {"priya.sharma@example.com", "Ananya Verma", "ananya.verma@gmail.com", "+91-98102-34502"}
        };
        String updateUserSql = "UPDATE users SET full_name = ?, email = ?, phone = ?, " +
                "member_id = (SELECT id FROM members WHERE email = ?) WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateUserSql)) {
            for (String[] u : userMigrations) {
                pstmt.setString(1, u[1]);
                pstmt.setString(2, u[2]);
                pstmt.setString(3, u[3]);
                pstmt.setString(4, u[2]);
                pstmt.setString(5, u[0]);
                pstmt.executeUpdate();
            }
        }

        // 3. Ensure sample student logins exist with standard password 'Member@123'
        String[][] demoStudents = {
                {"Aarav Sharma", "aarav.sharma@gmail.com", "+91-98101-23401"},
                {"Ananya Verma", "ananya.verma@gmail.com", "+91-98102-34502"},
                {"Rohan Mehta", "rohan.mehta@gmail.com", "+91-98103-45603"},
                {"Priya Malhotra", "priya.malhotra@gmail.com", "+91-98104-56704"}
        };

        String checkUserSql = "SELECT id FROM users WHERE email = ?";
        String insertUserSql = "INSERT INTO users (full_name, email, phone, password_hash, salt, role, member_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, 'MEMBER', (SELECT id FROM members WHERE email = ?), ?)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {

            for (String[] student : demoStudents) {
                String fullName = student[0];
                String email = student[1];
                String phone = student[2];

                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        String salt = PasswordUtils.generateSalt();
                        String hash = PasswordUtils.hashPassword("Member@123", salt);
                        insertStmt.setString(1, fullName);
                        insertStmt.setString(2, email);
                        insertStmt.setString(3, phone);
                        insertStmt.setString(4, hash);
                        insertStmt.setString(5, salt);
                        insertStmt.setString(6, email);
                        insertStmt.setString(7, now);
                        insertStmt.executeUpdate();
                    }
                }
            }
        }
    }
}
