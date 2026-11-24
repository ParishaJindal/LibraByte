package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database connection manager implementing standard JDBC patterns.
 */
public class DatabaseConnection {
    private static final String DB_DIR = "backend/database";
    private static final String DB_FILE = DB_DIR + "/librabyte.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            // Load SQLite JDBC Driver
            Class.forName("org.sqlite.JDBC");
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
        }
        return conn;
    }

    public static String getDbPath() {
        return DB_FILE;
    }
}
