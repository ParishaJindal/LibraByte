package server;

import com.sun.net.httpserver.HttpExchange;
import model.User;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Side Session Manager using pure Core Java.
 * Stores authenticated sessions in memory, issues secure HttpOnly cookies,
 * prevents session fixation, and enforces session expiration.
 */
public class SessionManager {
    public static final String COOKIE_NAME = "LIBRABYTE_SESSION";
    private static final int DEFAULT_SESSION_DURATION_SECONDS = 2 * 60 * 60; // 2 hours
    private static final int REMEMBER_ME_DURATION_SECONDS = 7 * 24 * 60 * 60; // 7 days

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, UserSession> ACTIVE_SESSIONS = new ConcurrentHashMap<>();

    public static class UserSession {
        private final String sessionId;
        private final int userId;
        private final String email;
        private final String fullName;
        private final String role; // "ADMIN" or "MEMBER"
        private final Integer memberId; // linked member ID if applicable
        private final long createdAt;
        private long expiresAt;
        private final boolean rememberMe;

        public UserSession(String sessionId, int userId, String email, String fullName, String role, Integer memberId, boolean rememberMe, int durationSeconds) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.email = email;
            this.fullName = fullName;
            this.role = role;
            this.memberId = memberId;
            this.rememberMe = rememberMe;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = this.createdAt + (durationSeconds * 1000L);
        }

        public String getSessionId() { return sessionId; }
        public int getUserId() { return userId; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public Integer getMemberId() { return memberId; }
        public boolean isRememberMe() { return rememberMe; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
        public void extend(int seconds) { this.expiresAt = System.currentTimeMillis() + (seconds * 1000L); }
        public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
        public boolean isMember() { return "MEMBER".equalsIgnoreCase(role); }
    }

    /**
     * Creates a new cryptographically secure session for a verified user (prevents session fixation).
     */
    public static UserSession createSession(User user, boolean rememberMe) {
        cleanExpiredSessions();

        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        int duration = rememberMe ? REMEMBER_ME_DURATION_SECONDS : DEFAULT_SESSION_DURATION_SECONDS;
        UserSession session = new UserSession(
                sessionId,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getMemberId(),
                rememberMe,
                duration
        );

        ACTIVE_SESSIONS.put(sessionId, session);
        return session;
    }

    /**
     * Retrieves an active session or null if missing/expired.
     */
    public static UserSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) return null;
        UserSession session = ACTIVE_SESSIONS.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                ACTIVE_SESSIONS.remove(sessionId);
                return null;
            }
            // Auto-extend non-expired session if not remember-me
            if (!session.isRememberMe()) {
                session.extend(DEFAULT_SESSION_DURATION_SECONDS);
            }
        }
        return session;
    }

    /**
     * Updates full name and email in the active in-memory session upon profile change.
     */
    public static void updateSessionDetails(String sessionId, String newFullName, String newEmail) {
        if (sessionId == null) return;
        UserSession old = ACTIVE_SESSIONS.get(sessionId);
        if (old != null && !old.isExpired()) {
            int remainingSeconds = (int) Math.max(60, (old.expiresAt - System.currentTimeMillis()) / 1000);
            UserSession updated = new UserSession(
                    old.getSessionId(),
                    old.getUserId(),
                    newEmail != null ? newEmail.trim() : old.getEmail(),
                    newFullName != null ? newFullName.trim() : old.getFullName(),
                    old.getRole(),
                    old.getMemberId(),
                    old.isRememberMe(),
                    remainingSeconds
            );
            ACTIVE_SESSIONS.put(sessionId, updated);
        }
    }

    /**
     * Destroys an active session.
     */
    public static void invalidateSession(String sessionId) {
        if (sessionId != null) {
            ACTIVE_SESSIONS.remove(sessionId);
        }
    }

    /**
     * Extracts session ID from incoming HTTP Cookie header.
     */
    public static String extractSessionId(HttpExchange exchange) {
        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");
        if (cookieHeaders == null || cookieHeaders.isEmpty()) return null;

        for (String header : cookieHeaders) {
            String[] cookies = header.split(";");
            for (String cookie : cookies) {
                String[] pair = cookie.trim().split("=", 2);
                if (pair.length == 2 && COOKIE_NAME.equalsIgnoreCase(pair[0])) {
                    return pair[1].trim();
                }
            }
        }
        return null;
    }

    /**
     * Resolves the current UserSession from the HttpExchange cookies.
     */
    public static UserSession resolveSession(HttpExchange exchange) {
        String sessionId = extractSessionId(exchange);
        return getSession(sessionId);
    }

    /**
     * Sets an HttpOnly, SameSite=Lax session cookie on the HTTP response.
     */
    public static void setSessionCookie(HttpExchange exchange, UserSession session) {
        int maxAge = session.isRememberMe() ? REMEMBER_ME_DURATION_SECONDS : DEFAULT_SESSION_DURATION_SECONDS;
        String cookieHeader = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                COOKIE_NAME, session.getSessionId(), maxAge);
        
        // Add Secure if request scheme or header indicates HTTPS
        String proto = exchange.getRequestHeaders().getFirst("X-Forwarded-Proto");
        if ("https".equalsIgnoreCase(proto)) {
            cookieHeader += "; Secure";
        }
        exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);
    }

    /**
     * Clears the session cookie on logout.
     */
    public static void clearSessionCookie(HttpExchange exchange) {
        String cookieHeader = String.format("%s=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax", COOKIE_NAME);
        exchange.getResponseHeaders().add("Set-Cookie", cookieHeader);
    }

    private static void cleanExpiredSessions() {
        ACTIVE_SESSIONS.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public static int getActiveSessionCount() {
        cleanExpiredSessions();
        return ACTIVE_SESSIONS.size();
    }
}
