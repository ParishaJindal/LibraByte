import dao.BookDao;
import dao.IssueDao;
import dao.MemberDao;
import dao.ReservationDao;
import dao.UserDao;
import database.DatabaseInitializer;
import server.ApiServer;
import service.*;

/**
 * Main application entry point for LibraByte Library Queue Optimizer.
 * Initializes the database, services, in-memory FIFO reservation queues, and HTTP REST server.
 */
public class Main {
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("       LIBRABYTE - LIBRARY QUEUE OPTIMIZER       ");
        System.out.println("      Core Java & JDBC Web Application Backend   ");
        System.out.println("=================================================");

        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port specified, defaulting to " + DEFAULT_PORT);
            }
        }

        // 1. Initialize SQLite database & seed demo records if new
        DatabaseInitializer.initializeDatabase();

        // 2. Initialize Data Access Objects
        BookDao bookDao = new BookDao();
        MemberDao memberDao = new MemberDao();
        IssueDao issueDao = new IssueDao();
        ReservationDao reservationDao = new ReservationDao();
        UserDao userDao = new UserDao();

        // 3. Initialize Services with collections & FIFO queues
        BookService bookService = new BookService(bookDao);
        MemberService memberService = new MemberService(memberDao);
        ReservationService reservationService = new ReservationService(reservationDao, bookDao, memberDao);
        LibraryService libraryService = new LibraryService(issueDao, bookDao, memberDao, bookService, reservationService);
        ReportService reportService = new ReportService(issueDao, bookDao, memberDao, reservationDao);
        AuthService authService = new AuthService(userDao, memberDao, issueDao, reservationDao);

        // 4. Start HTTP Server
        try {
            ApiServer server = new ApiServer(port, bookService, memberService, libraryService, reservationService, reportService, authService);
            server.start();

            System.out.println("\nApplication is ready!");
            System.out.println("Dashboard & UI : http://localhost:" + port + "/");
            System.out.println("REST API Base  : http://localhost:" + port + "/api/");
            System.out.println("Press Ctrl+C to terminate.\n");

            // Add graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down LibraByte Server...");
                server.stop();
            }));

        } catch (Exception e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
