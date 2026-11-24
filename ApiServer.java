package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import exceptions.LibraryException;
import model.*;
import server.SessionManager.UserSession;
import service.*;
import util.JsonUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Built-in REST API and Static File Server using com.sun.net.httpserver.HttpServer.
 * No external frameworks; pure Core Java implementation.
 * Enforces Role-Based Access Control (RBAC) with ADMIN and MEMBER roles.
 */
public class ApiServer {
    private final int port;
    private final BookService bookService;
    private final MemberService memberService;
    private final LibraryService libraryService;
    private final ReservationService reservationService;
    private final ReportService reportService;
    private final AuthService authService;
    private HttpServer server;

    public ApiServer(int port, BookService bookService, MemberService memberService,
                     LibraryService libraryService, ReservationService reservationService,
                     ReportService reportService, AuthService authService) {
        this.port = port;
        this.bookService = bookService;
        this.memberService = memberService;
        this.libraryService = libraryService;
        this.reservationService = reservationService;
        this.reportService = reportService;
        this.authService = authService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Contexts
        server.createContext("/api/auth", this::handleAuth);
        server.createContext("/api/dashboard", this::handleDashboard);
        server.createContext("/api/members", this::handleMembers);
        server.createContext("/api/books", this::handleBooks);
        server.createContext("/api/issues", this::handleIssues);
        server.createContext("/api/reservations", this::handleReservations);
        server.createContext("/api/reports", this::handleReports);
        server.createContext("/api/profile", this::handleProfile);

        // Static Frontend Files Context
        server.createContext("/", this::handleStaticFiles);

        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("LibraByte Server running on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("LibraByte Server stopped.");
        }
    }

    // -------------------------------------------------------------
    // Authentication Handler
    // -------------------------------------------------------------
    private void handleAuth(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if (path.endsWith("/login") && "POST".equals(method)) {
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                String email = (String) map.get("email");
                String password = (String) map.get("password");
                boolean rememberMe = Boolean.TRUE.equals(map.get("rememberMe"));

                Map<String, Object> res = authService.login(email, password, rememberMe, exchange);
                HttpUtils.sendJsonResponse(exchange, 200, res);
            } else if (path.endsWith("/signup") && "POST".equals(method)) {
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                String fullName = (String) map.get("fullName");
                String email = (String) map.get("email");
                String phone = (String) map.get("phone");
                String password = (String) map.get("password");
                String confirmPassword = (String) map.get("confirmPassword");
                String role = (String) map.get("role");
                if (role == null || role.trim().isEmpty()) {
                    role = "MEMBER";
                }

                User user = authService.register(fullName, email, phone, password, confirmPassword, role);
                HttpUtils.sendJsonResponse(exchange, 201, user);
            } else if (path.endsWith("/logout") && "POST".equals(method)) {
                authService.logout(exchange);
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("message", "Logged out successfully");
                HttpUtils.sendJsonResponse(exchange, 200, res);
            } else if (path.endsWith("/me") && "GET".equals(method)) {
                Map<String, Object> user = authService.getCurrentUser(exchange);
                if (user == null) {
                    HttpUtils.sendErrorResponse(exchange, 401, "Not authenticated");
                    return;
                }
                HttpUtils.sendJsonResponse(exchange, 200, user);
            } else {
                HttpUtils.sendErrorResponse(exchange, 404, "Auth endpoint not found: " + path);
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Dashboard Handler (ADMIN only)
    // -------------------------------------------------------------
    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        try {
            authService.requireAdmin(exchange);
            DashboardStats stats = reportService.getDashboardStats();
            HttpUtils.sendJsonResponse(exchange, 200, stats);
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Database error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Members Handler (ADMIN manages all; MEMBER may view own profile)
    // -------------------------------------------------------------
    private void handleMembers(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                Integer id = HttpUtils.extractIdFromPath(path, "/api/members");
                if (id != null) {
                    UserSession session = authService.requireAuth(exchange);
                    if (!session.isAdmin()) {
                        if (session.getMemberId() == null || session.getMemberId() != id) {
                            HttpUtils.sendErrorResponse(exchange, 403, "Forbidden: You may only view your own profile.");
                            return;
                        }
                    }
                    Member member = memberService.getMemberById(id);
                    HttpUtils.sendJsonResponse(exchange, 200, member);
                } else {
                    authService.requireAdmin(exchange);
                    Map<String, String> params = HttpUtils.parseQueryParams(exchange);
                    String search = params.get("search");
                    List<Member> list = memberService.searchMembers(search);
                    HttpUtils.sendJsonResponse(exchange, 200, list);
                }
            } else if ("POST".equals(method)) {
                authService.requireAdmin(exchange);
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                Member member = new Member();
                member.setFullName((String) map.get("fullName"));
                member.setEmail((String) map.get("email"));
                member.setPhone((String) map.get("phone"));
                member.setStatus((String) map.get("status"));
                member.setRegistrationDate((String) map.get("registrationDate"));

                Member created = memberService.registerMember(member);
                HttpUtils.sendJsonResponse(exchange, 201, created);
            } else if ("PUT".equals(method)) {
                authService.requireAdmin(exchange);
                Integer id = HttpUtils.extractIdFromPath(path, "/api/members");
                if (id == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Member ID required in URL path.");
                    return;
                }
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                Member member = new Member();
                member.setId(id);
                member.setFullName((String) map.get("fullName"));
                member.setEmail((String) map.get("email"));
                member.setPhone((String) map.get("phone"));
                member.setStatus((String) map.get("status"));

                Member updated = memberService.updateMember(member);
                HttpUtils.sendJsonResponse(exchange, 200, updated);
            } else if ("DELETE".equals(method)) {
                authService.requireAdmin(exchange);
                Integer id = HttpUtils.extractIdFromPath(path, "/api/members");
                if (id == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Member ID required in URL path.");
                    return;
                }
                boolean deleted = memberService.deleteMember(id);
                Map<String, Object> res = new HashMap<>();
                res.put("success", deleted);
                res.put("message", "Member successfully deleted.");
                HttpUtils.sendJsonResponse(exchange, 200, res);
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Books Handler (Public/Member can view & search; ADMIN modifies)
    // -------------------------------------------------------------
    private void handleBooks(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                Integer id = HttpUtils.extractIdFromPath(path, "/api/books");
                if (id != null) {
                    Book book = bookService.getBookById(id);
                    HttpUtils.sendJsonResponse(exchange, 200, book);
                } else {
                    Map<String, String> params = HttpUtils.parseQueryParams(exchange);
                    String search = params.get("search");
                    List<Book> list = bookService.searchBooks(search);
                    HttpUtils.sendJsonResponse(exchange, 200, list);
                }
            } else if ("POST".equals(method)) {
                authService.requireAdmin(exchange);
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                Book book = new Book();
                book.setIsbn((String) map.get("isbn"));
                book.setTitle((String) map.get("title"));
                book.setAuthor((String) map.get("author"));
                book.setCategory((String) map.get("category"));
                book.setPublisher((String) map.get("publisher"));
                if (map.get("totalCopies") instanceof Number) {
                    book.setTotalCopies(((Number) map.get("totalCopies")).intValue());
                }
                if (map.get("availableCopies") instanceof Number) {
                    book.setAvailableCopies(((Number) map.get("availableCopies")).intValue());
                } else {
                    book.setAvailableCopies(book.getTotalCopies());
                }

                Book created = bookService.addBook(book);
                HttpUtils.sendJsonResponse(exchange, 201, created);
            } else if ("PUT".equals(method)) {
                authService.requireAdmin(exchange);
                Integer id = HttpUtils.extractIdFromPath(path, "/api/books");
                if (id == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Book ID required in URL path.");
                    return;
                }
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                Book book = new Book();
                book.setId(id);
                book.setIsbn((String) map.get("isbn"));
                book.setTitle((String) map.get("title"));
                book.setAuthor((String) map.get("author"));
                book.setCategory((String) map.get("category"));
                book.setPublisher((String) map.get("publisher"));
                if (map.get("totalCopies") instanceof Number) {
                    book.setTotalCopies(((Number) map.get("totalCopies")).intValue());
                }

                Book updated = bookService.updateBook(book);
                HttpUtils.sendJsonResponse(exchange, 200, updated);
            } else if ("DELETE".equals(method)) {
                authService.requireAdmin(exchange);
                Integer id = HttpUtils.extractIdFromPath(path, "/api/books");
                if (id == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Book ID required in URL path.");
                    return;
                }
                boolean deleted = bookService.deleteBook(id);
                Map<String, Object> res = new HashMap<>();
                res.put("success", deleted);
                res.put("message", "Book successfully deleted.");
                HttpUtils.sendJsonResponse(exchange, 200, res);
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Issues Handler (ADMIN issues/returns; MEMBER views their own loans)
    // -------------------------------------------------------------
    private void handleIssues(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                UserSession session = authService.requireAuth(exchange);
                if (session.isAdmin()) {
                    List<IssueRecord> list = libraryService.getAllIssues();
                    HttpUtils.sendJsonResponse(exchange, 200, list);
                } else {
                    if (session.getMemberId() == null) {
                        HttpUtils.sendJsonResponse(exchange, 200, Collections.emptyList());
                    } else {
                        List<IssueRecord> list = libraryService.getIssuesByMemberId(session.getMemberId());
                        HttpUtils.sendJsonResponse(exchange, 200, list);
                    }
                }
            } else if ("POST".equals(method)) {
                authService.requireAdmin(exchange);
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);

                if (path.endsWith("/return")) {
                    // Return Book
                    Object issueIdObj = map.get("issueId");
                    if (issueIdObj == null || !(issueIdObj instanceof Number)) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Valid issueId is required for returning.");
                        return;
                    }
                    int issueId = ((Number) issueIdObj).intValue();
                    Map<String, Object> result = libraryService.returnBook(issueId);
                    HttpUtils.sendJsonResponse(exchange, 200, result);
                } else {
                    // Issue Book
                    Object bookIdObj = map.get("bookId");
                    Object memberIdObj = map.get("memberId");
                    if (bookIdObj == null || memberIdObj == null) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Both bookId and memberId are required.");
                        return;
                    }
                    int bookId = ((Number) bookIdObj).intValue();
                    int memberId = ((Number) memberIdObj).intValue();

                    IssueRecord issue = libraryService.issueBook(bookId, memberId);
                    HttpUtils.sendJsonResponse(exchange, 201, issue);
                }
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Reservations Handler (FIFO Queue & Member reservations)
    // -------------------------------------------------------------
    private void handleReservations(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                UserSession session = authService.requireAuth(exchange);
                Map<String, String> params = HttpUtils.parseQueryParams(exchange);
                String bookIdStr = params.get("bookId");
                if (bookIdStr != null && !bookIdStr.isEmpty()) {
                    int bookId = Integer.parseInt(bookIdStr);
                    List<Reservation> queue = reservationService.getActiveQueueForBook(bookId);
                    HttpUtils.sendJsonResponse(exchange, 200, queue);
                } else if (session.isAdmin()) {
                    List<Reservation> all = reservationService.getAllReservations();
                    HttpUtils.sendJsonResponse(exchange, 200, all);
                } else {
                    if (session.getMemberId() == null) {
                        HttpUtils.sendJsonResponse(exchange, 200, Collections.emptyList());
                    } else {
                        List<Reservation> myRes = reservationService.getReservationsByMemberId(session.getMemberId());
                        HttpUtils.sendJsonResponse(exchange, 200, myRes);
                    }
                }
            } else if ("POST".equals(method)) {
                UserSession session = authService.requireAuth(exchange);
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> map = JsonUtils.parseJsonObject(body);
                Object bookIdObj = map.get("bookId");
                Object memberIdObj = map.get("memberId");

                if (bookIdObj == null || !(bookIdObj instanceof Number)) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Valid bookId is required to reserve.");
                    return;
                }
                int bookId = ((Number) bookIdObj).intValue();
                int memberId;

                if (session.isAdmin()) {
                    if (memberIdObj == null || !(memberIdObj instanceof Number)) {
                        HttpUtils.sendErrorResponse(exchange, 400, "Member ID is required for admin reservations.");
                        return;
                    }
                    memberId = ((Number) memberIdObj).intValue();
                } else {
                    if (session.getMemberId() == null) {
                        HttpUtils.sendErrorResponse(exchange, 400, "No linked member profile for current user.");
                        return;
                    }
                    memberId = session.getMemberId();
                }

                Reservation res = reservationService.reserveBook(bookId, memberId);
                HttpUtils.sendJsonResponse(exchange, 201, res);
            } else if ("DELETE".equals(method)) {
                UserSession session = authService.requireAuth(exchange);
                Integer id = HttpUtils.extractIdFromPath(path, "/api/reservations");
                if (id == null) {
                    HttpUtils.sendErrorResponse(exchange, 400, "Reservation ID required in URL path.");
                    return;
                }
                if (!session.isAdmin()) {
                    Reservation existing = reservationService.getAllReservations().stream()
                            .filter(r -> r.getId() == id)
                            .findFirst()
                            .orElse(null);
                    if (existing != null && session.getMemberId() != null && existing.getMemberId() != session.getMemberId()) {
                        HttpUtils.sendErrorResponse(exchange, 403, "Forbidden: You may only cancel your own reservations.");
                        return;
                    }
                }
                boolean cancelled = reservationService.cancelReservation(id);
                Map<String, Object> res = new HashMap<>();
                res.put("success", cancelled);
                res.put("message", "Reservation cancelled and queue positions updated.");
                HttpUtils.sendJsonResponse(exchange, 200, res);
            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Reports Handler (ADMIN only)
    // -------------------------------------------------------------
    private void handleReports(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String path = exchange.getRequestURI().getPath();

        try {
            authService.requireAdmin(exchange);
            if (path.endsWith("/overdue")) {
                List<OverdueReportItem> list = reportService.getOverdueReport();
                HttpUtils.sendJsonResponse(exchange, 200, list);
            } else if (path.endsWith("/popular-books")) {
                List<PopularBookReportItem> list = reportService.getPopularBooksReport();
                HttpUtils.sendJsonResponse(exchange, 200, list);
            } else if (path.endsWith("/reservations")) {
                List<Reservation> list = reportService.getReservationReport();
                HttpUtils.sendJsonResponse(exchange, 200, list);
            } else {
                HttpUtils.sendErrorResponse(exchange, 404, "Report type not found. Use /overdue, /popular-books, or /reservations.");
            }
        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (SQLException e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Database error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Personal Information & Profile Handler (Both ADMIN and MEMBER)
    // -------------------------------------------------------------
    private void handleProfile(HttpExchange exchange) throws IOException {
        if (HttpUtils.handlePreflight(exchange)) return;
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            // Strictly require valid authenticated session
            UserSession session = authService.requireAuth(exchange);
            int userId = session.getUserId();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> profile = authService.getProfile(userId);
                HttpUtils.sendJsonResponse(exchange, 200, profile);

            } else if ("PUT".equalsIgnoreCase(method) && (path.endsWith("/password") || path.endsWith("/password/"))) {
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> data = JsonUtils.parseJsonObject(body);
                String currentPassword = (String) data.get("currentPassword");
                String newPassword = (String) data.get("newPassword");
                String confirmPassword = (String) data.get("confirmPassword");

                authService.changePassword(userId, currentPassword, newPassword, confirmPassword);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Password changed successfully.");
                HttpUtils.sendJsonResponse(exchange, 200, resp);

            } else if ("PUT".equalsIgnoreCase(method)) {
                String body = HttpUtils.readRequestBody(exchange);
                Map<String, Object> data = JsonUtils.parseJsonObject(body);
                String fullName = (String) data.get("fullName");
                String email = (String) data.get("email");
                String phone = (String) data.get("phone");
                String profileImage = (String) data.get("profileImage");

                Map<String, Object> updated = authService.updateProfile(userId, fullName, email, phone, profileImage, session.getSessionId());
                HttpUtils.sendJsonResponse(exchange, 200, updated);

            } else {
                HttpUtils.sendErrorResponse(exchange, 405, "Method Not Allowed");
            }

        } catch (LibraryException e) {
            HttpUtils.sendErrorResponse(exchange, e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            HttpUtils.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------
    // Static File Server Handler
    // -------------------------------------------------------------
    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        HttpUtils.setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent directory traversal attacks
        if (path.contains("..")) {
            HttpUtils.sendErrorResponse(exchange, 403, "Forbidden");
            return;
        }

        File file = new File("frontend" + path);
        if (!file.exists() || file.isDirectory()) {
            // Check if user requested a page without .html
            File htmlFile = new File("frontend" + path + ".html");
            if (htmlFile.exists() && !htmlFile.isDirectory()) {
                file = htmlFile;
            } else {
                HttpUtils.sendErrorResponse(exchange, 404, "File Not Found: " + path);
                return;
            }
        }

        String mimeType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getMimeType(String filename) {
        if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
