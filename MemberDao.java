package dao;

import database.DatabaseConnection;
import model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDao {

    public List<Member> findAll() throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                members.add(mapResultSetToMember(rs));
            }
        }
        return members;
    }

    public Member findById(int id) throws SQLException {
        String sql = "SELECT * FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
        }
        return null;
    }

    public Member findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM members WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
        }
        return null;
    }

    public List<Member> search(String query) throws SQLException {
        List<Member> members = new ArrayList<>();
        String sql = "SELECT * FROM members WHERE full_name LIKE ? OR email LIKE ? OR phone LIKE ? OR CAST(id AS TEXT) LIKE ? ORDER BY full_name ASC";
        String pattern = "%" + query.trim() + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            pstmt.setString(4, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSetToMember(rs));
                }
            }
        }
        return members;
    }

    public int insert(Member member) throws SQLException {
        String sql = "INSERT INTO members (full_name, email, phone, registration_date, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, member.getFullName().trim());
            pstmt.setString(2, member.getEmail().trim().toLowerCase());
            pstmt.setString(3, member.getPhone().trim());
            pstmt.setString(4, member.getRegistrationDate());
            pstmt.setString(5, member.getStatus());
            pstmt.setString(6, member.getCreatedAt());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    member.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public boolean update(Member member) throws SQLException {
        String sql = "UPDATE members SET full_name = ?, email = ?, phone = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, member.getFullName().trim());
            pstmt.setString(2, member.getEmail().trim().toLowerCase());
            pstmt.setString(3, member.getPhone().trim());
            pstmt.setString(4, member.getStatus());
            pstmt.setInt(5, member.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean hasActiveIssuesOrReservations(int memberId) throws SQLException {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM issue_records WHERE member_id = ? AND status IN ('ISSUED', 'OVERDUE')) + " +
                "(SELECT COUNT(*) FROM reservations WHERE member_id = ? AND status = 'WAITING') AS total_active";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_active") > 0;
                }
            }
        }
        return false;
    }

    private Member mapResultSetToMember(ResultSet rs) throws SQLException {
        Member member = new Member(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("registration_date"),
                rs.getString("status")
        );
        member.setCreatedAt(rs.getString("created_at"));
        return member;
    }
}
