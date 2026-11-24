package service;

import com.sun.net.httpserver.HttpExchange;
import dao.IssueDao;
import dao.MemberDao;
import dao.ReservationDao;
import dao.UserDao;
import exceptions.LibraryException;
import model.IssueRecord;
import model.Member;
import model.Reservation;
import model.User;
import server.SessionManager;
import server.SessionManager.UserSession;
import util.DateUtils;
import util.PasswordUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Enterprise Authentication Service implementing PBKDF2 password hashing,
 * server-side sessions, user registration, Role-Based Access Control (RBAC),
 * and Personal Information / Password Management.
 */
public class AuthService {
    private final UserDao userDao;
    private final MemberDao memberDao;
    private final IssueDao issueDao;
    private final ReservationDao reservationDao;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");

    public AuthService(UserDao userDao, MemberDao memberDao) {
        this(userDao, memberDao, new IssueDao(), new ReservationDao());
    }

    public AuthService(UserDao userDao, MemberDao memberDao, IssueDao issueDao, ReservationDao reservationDao) {
        this.userDao = userDao;
        this.memberDao = memberDao;
        this.issueDao = issueDao;
        this.reservationDao = reservationDao;
    }

    /**
     * Registers a new user account with salted PBKDF2 password hashing.
     * Prevents duplicate emails and enforces security policies.
     */
    public synchronized User register(String fullName, String email, String phone, String password, String confirmPassword, String role) throws LibraryException {
        // 1. Validations
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new LibraryException("Full name is required.", 400);
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new LibraryException("Please provide a valid email address.", 400);
        }
        if (phone == null || phone.trim().length() < 7) {
            throw new LibraryException("Please provide a valid phone number.", 400);
        }
        if (password == null || password.length() < 6) {
            throw new LibraryException("Password must be at least 6 characters long.", 400);
        }
        if (!password.equals(confirmPassword)) {
            throw new LibraryException("Passwords do not match.", 400);
        }

        String assignedRole = "MEMBER";
        if (role != null && "ADMIN".equalsIgnoreCase(role.trim())) {
            assignedRole = "ADMIN";
        }

        try {
            // 2. Prevent duplicate email registrations
            User existing = userDao.findByEmail(email);
            if (existing != null) {
                throw new LibraryException("An account with email '" + email + "' already exists.", 409);
            }

            // 3. PBKDF2WithHmacSHA256 salted hashing
            String salt = PasswordUtils.generateSalt();
            String hash = PasswordUtils.hashPassword(password, salt);
            String now = DateUtils.now();

            // 4. If registering as a MEMBER, link or create associated patron in members table
            Integer memberId = null;
            if ("MEMBER".equals(assignedRole)) {
                Member existingMember = memberDao.findByEmail(email);
                if (existingMember != null) {
                    memberId = existingMember.getId();
                } else {
                    Member newMember = new Member(0, fullName.trim(), email.trim(), phone.trim(), DateUtils.today(), "ACTIVE");
                    newMember.setCreatedAt(now);
                    memberId = memberDao.insert(newMember);
                }
            }

            // 5. Persist user in database
            User user = new User(0, fullName.trim(), email.trim(), phone.trim(), hash, salt, assignedRole, memberId, now);
            int userId = userDao.insert(user);
            user.setId(userId);

            // Strip sensitive credentials before returning
            user.setPasswordHash(null);
            user.setSalt(null);
            return user;

        } catch (SQLException e) {
            throw new LibraryException("Database error during registration: " + e.getMessage(), 500);
        }
    }

    /**
     * Authenticates user credentials and establishes a server-side session with HttpOnly cookie.
     */
    public synchronized Map<String, Object> login(String email, String password, boolean rememberMe, HttpExchange exchange) throws LibraryException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new LibraryException("Email and password are required.", 400);
        }

        try {
            User user = userDao.findByEmail(email.trim());
            if (user == null) {
                throw new LibraryException("Invalid email or password.", 401);
            }

            // Verify salted PBKDF2 hash
            boolean matches = PasswordUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash());
            if (!matches) {
                throw new LibraryException("Invalid email or password.", 401);
            }

            // Create cryptographically strong session & set HttpOnly cookie
            UserSession session = SessionManager.createSession(user, rememberMe);
            SessionManager.setSessionCookie(exchange, session);

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("role", user.getRole());
            response.put("memberId", user.getMemberId());
            response.put("sessionId", session.getSessionId());

            return response;

        } catch (SQLException e) {
            throw new LibraryException("Database error during login: " + e.getMessage(), 500);
        }
    }

    /**
     * Logs out the user by invalidating the active session and wiping the cookie.
     */
    public void logout(HttpExchange exchange) {
        String sessionId = SessionManager.extractSessionId(exchange);
        SessionManager.invalidateSession(sessionId);
        SessionManager.clearSessionCookie(exchange);
    }

    /**
     * Resolves currently authenticated user from incoming HTTP session.
     */
    public Map<String, Object> getCurrentUser(HttpExchange exchange) {
        UserSession session = SessionManager.resolveSession(exchange);
        if (session == null) {
            return null;
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getUserId());
        map.put("fullName", session.getFullName());
        map.put("email", session.getEmail());
        map.put("role", session.getRole());
        map.put("memberId", session.getMemberId());
        return map;
    }

    /**
     * Guard: Requires valid authentication (any role).
     */
    public UserSession requireAuth(HttpExchange exchange) throws LibraryException {
        UserSession session = SessionManager.resolveSession(exchange);
        if (session == null) {
            throw new LibraryException("Unauthorized: Please log in to access this resource.", 401);
        }
        return session;
    }

    /**
     * Guard: Requires ADMIN privileges.
     */
    public UserSession requireAdmin(HttpExchange exchange) throws LibraryException {
        UserSession session = requireAuth(exchange);
        if (!session.isAdmin()) {
            throw new LibraryException("Forbidden: Administrator privileges required for this action.", 403);
        }
        return session;
    }

    // =========================================================================
    // PERSONAL INFORMATION MANAGEMENT & SECURITY
    // =========================================================================

    /**
     * Fetches personal profile for the authenticated user ID.
     * If user is a MEMBER, populates active loans, borrowing history, and reservations.
     */
    public Map<String, Object> getProfile(int userId) throws LibraryException {
        try {
            User user = userDao.findById(userId);
            if (user == null) {
                throw new LibraryException("User account not found.", 404);
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", user.getId());
            profile.put("fullName", user.getFullName());
            profile.put("email", user.getEmail());
            profile.put("phone", user.getPhone());
            profile.put("role", user.getRole());
            profile.put("memberId", user.getMemberId());
            profile.put("createdAt", user.getCreatedAt());
            profile.put("status", "ACTIVE");
            profile.put("profileImage", user.getProfileImage() != null ? user.getProfileImage() : "");

            // If user is a MEMBER with a linked member ID, fetch patron loan & queue history
            if (user.isMember() && user.getMemberId() != null) {
                if (issueDao != null) {
                    List<IssueRecord> allLoans = issueDao.findByMemberId(user.getMemberId());
                    List<IssueRecord> activeLoans = new ArrayList<>();
                    List<IssueRecord> borrowingHistory = new ArrayList<>();
                    for (IssueRecord ir : allLoans) {
                        if ("RETURNED".equalsIgnoreCase(ir.getStatus())) {
                            borrowingHistory.add(ir);
                        } else {
                            activeLoans.add(ir);
                        }
                    }
                    profile.put("activeLoans", activeLoans);
                    profile.put("borrowingHistory", borrowingHistory);
                } else {
                    profile.put("activeLoans", new ArrayList<>());
                    profile.put("borrowingHistory", new ArrayList<>());
                }

                if (reservationDao != null) {
                    List<Reservation> allRes = reservationDao.findByMemberId(user.getMemberId());
                    List<Reservation> activeRes = new ArrayList<>();
                    for (Reservation r : allRes) {
                        if ("WAITING".equalsIgnoreCase(r.getStatus()) || "ALLOCATED".equalsIgnoreCase(r.getStatus())) {
                            activeRes.add(r);
                        }
                    }
                    profile.put("activeReservations", activeRes);
                } else {
                    profile.put("activeReservations", new ArrayList<>());
                }
            }

            return profile;
        } catch (SQLException e) {
            throw new LibraryException("Database error loading profile: " + e.getMessage(), 500);
        }
    }

    /**
     * Updates personal information for the authenticated user.
     * Prevents duplicate emails, validates phone numbers, prevents role tampering,
     * keeps linked patron record synchronized, and refreshes session cache.
     */
    public synchronized Map<String, Object> updateProfile(int userId, String fullName, String email, String phone, String profileImage, String sessionId) throws LibraryException {
        // 1. Data Validations
        if (fullName == null || fullName.trim().length() < 2) {
            throw new LibraryException("Full name must be at least 2 characters long.", 400);
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new LibraryException("Please provide a valid email address.", 400);
        }
        if (!isValidIndianPhone(phone)) {
            throw new LibraryException("Please provide a valid 10-digit Indian mobile number (e.g. +91-98765-43210 or 9876543210).", 400);
        }

        try {
            User existing = userDao.findById(userId);
            if (existing == null) {
                throw new LibraryException("User account not found.", 404);
            }

            // 2. Prevent duplicate email addresses across users
            if (userDao.isEmailTakenByOther(email.trim(), userId)) {
                throw new LibraryException("An account with email '" + email.trim() + "' already exists.", 409);
            }

            // Also check members table if linked to avoid conflicts
            if (existing.getMemberId() != null) {
                Member existingMember = memberDao.findByEmail(email.trim());
                if (existingMember != null && existingMember.getId() != existing.getMemberId()) {
                    throw new LibraryException("A library member with email '" + email.trim() + "' already exists.", 409);
                }
            }

            String formattedPhone = normalizeIndianPhone(phone);
            String imageVal = (profileImage != null) ? profileImage.trim() : existing.getProfileImage();

            // 3. Update Users table (Role is strictly preserved from database, never modifiable!)
            boolean updated = userDao.updateProfile(userId, fullName.trim(), email.trim(), formattedPhone, imageVal);
            if (!updated) {
                throw new LibraryException("Failed to update user profile in database.", 500);
            }

            // 4. Synchronize linked member directory record if applicable
            if (existing.getMemberId() != null) {
                userDao.updateLinkedMember(existing.getMemberId(), fullName.trim(), email.trim(), formattedPhone);
            }

            // 5. Update session details in memory
            if (sessionId != null) {
                SessionManager.updateSessionDetails(sessionId, fullName.trim(), email.trim());
            }

            return getProfile(userId);

        } catch (SQLException e) {
            throw new LibraryException("Database error updating personal profile: " + e.getMessage(), 500);
        }
    }

    /**
     * Changes password for the authenticated user.
     * Verifies existing PBKDF2 password, hashes new password with fresh salt,
     * and securely persists to database.
     */
    public synchronized void changePassword(int userId, String currentPassword, String newPassword, String confirmPassword) throws LibraryException {
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new LibraryException("Current password is required.", 400);
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new LibraryException("New password must be at least 6 characters long.", 400);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new LibraryException("New passwords do not match.", 400);
        }

        try {
            User user = userDao.findById(userId);
            if (user == null) {
                throw new LibraryException("User account not found.", 404);
            }

            // Verify current password with PBKDF2 hash
            boolean matches = PasswordUtils.verifyPassword(currentPassword, user.getSalt(), user.getPasswordHash());
            if (!matches) {
                throw new LibraryException("Current password is incorrect.", 400);
            }

            // Hash new password with newly generated cryptographic salt
            String newSalt = PasswordUtils.generateSalt();
            String newHash = PasswordUtils.hashPassword(newPassword, newSalt);

            boolean updated = userDao.updatePassword(userId, newHash, newSalt);
            if (!updated) {
                throw new LibraryException("Failed to update password in database.", 500);
            }

        } catch (SQLException e) {
            throw new LibraryException("Database error changing password: " + e.getMessage(), 500);
        }
    }

    /**
     * Validates Indian mobile numbers (10 digits, starts with 6-9, with optional +91 prefix).
     */
    public static boolean isValidIndianPhone(String phone) {
        if (phone == null) return false;
        String clean = phone.trim().replaceAll("[^0-9]", "");
        if (clean.length() == 12 && clean.startsWith("91")) {
            clean = clean.substring(2);
        } else if (clean.length() == 11 && clean.startsWith("0")) {
            clean = clean.substring(1);
        }
        return clean.length() == 10 && clean.matches("^[6-9]\\d{9}$");
    }

    /**
     * Normalizes Indian mobile number into standard +91-XXXXX-XXXXX representation.
     */
    public static String normalizeIndianPhone(String phone) {
        if (phone == null) return "";
        String clean = phone.trim().replaceAll("[^0-9]", "");
        if (clean.length() == 12 && clean.startsWith("91")) {
            clean = clean.substring(2);
        } else if (clean.length() == 11 && clean.startsWith("0")) {
            clean = clean.substring(1);
        }
        if (clean.length() == 10) {
            return "+91-" + clean.substring(0, 5) + "-" + clean.substring(5);
        }
        return phone.trim();
    }
}
