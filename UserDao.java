package dao;

import database.DatabaseConnection;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    public int insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, phone, password_hash, salt, role, member_id, created_at, profile_image) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getFullName().trim());
            pstmt.setString(2, user.getEmail().trim().toLowerCase());
            pstmt.setString(3, user.getPhone().trim());
            pstmt.setString(4, user.getPasswordHash());
            pstmt.setString(5, user.getSalt());
            pstmt.setString(6, user.getRole().toUpperCase());
            if (user.getMemberId() != null) {
                pstmt.setInt(7, user.getMemberId());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setString(8, user.getCreatedAt());
            pstmt.setString(9, user.getProfileImage() != null ? user.getProfileImage() : "");
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int genId = keys.getInt(1);
                    user.setId(genId);
                    return genId;
                }
            }
        }
        return -1;
    }

    public int insert(User user) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return insert(conn, user);
        }
    }

    public boolean update(User user) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, phone = ?, role = ?, profile_image = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getFullName().trim());
            pstmt.setString(2, user.getPhone().trim());
            pstmt.setString(3, user.getRole());
            pstmt.setString(4, user.getProfileImage() != null ? user.getProfileImage() : "");
            pstmt.setInt(5, user.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateProfile(int userId, String fullName, String email, String phone, String profileImage) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, profile_image = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName.trim());
            pstmt.setString(2, email.trim().toLowerCase());
            pstmt.setString(3, phone.trim());
            pstmt.setString(4, profileImage != null ? profileImage.trim() : "");
            pstmt.setInt(5, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateLinkedMember(int memberId, String fullName, String email, String phone) throws SQLException {
        String sql = "UPDATE members SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fullName.trim());
            pstmt.setString(2, email.trim().toLowerCase());
            pstmt.setString(3, phone.trim());
            pstmt.setInt(4, memberId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean isEmailTakenByOther(String email, int excludeUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) AND id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            pstmt.setInt(2, excludeUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean updatePassword(int userId, String newHash, String newSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newHash);
            pstmt.setString(2, newSalt);
            pstmt.setInt(3, userId);
            return pstmt.executeUpdate() > 0;
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        Integer memberId = rs.getInt("member_id");
        if (rs.wasNull()) {
            memberId = null;
        }
        String profileImage = "";
        try {
            profileImage = rs.getString("profile_image");
            if (profileImage == null) profileImage = "";
        } catch (SQLException ignored) {}

        return new User(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                rs.getString("role"),
                memberId,
                rs.getString("created_at"),
                profileImage
        );
    }
}
