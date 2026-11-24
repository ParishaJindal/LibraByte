package dao;

import database.DatabaseConnection;
import model.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {

    public List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, b.title as book_title, b.author as book_author, m.full_name as member_name, m.email as member_email " +
                     "FROM reservations r " +
                     "JOIN books b ON r.book_id = b.id " +
                     "JOIN members m ON r.member_id = m.id " +
                     "ORDER BY r.book_id ASC, r.queue_position ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        }
        return list;
    }

    public List<Reservation> findActiveByBookId(int bookId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, b.title as book_title, b.author as book_author, m.full_name as member_name, m.email as member_email " +
                     "FROM reservations r " +
                     "JOIN books b ON r.book_id = b.id " +
                     "JOIN members m ON r.member_id = m.id " +
                     "WHERE r.book_id = ? AND r.status = 'WAITING' " +
                     "ORDER BY r.queue_position ASC, r.reservation_date ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
            }
        }
        return list;
    }

    public Reservation findById(int id) throws SQLException {
        String sql = "SELECT r.*, b.title as book_title, b.author as book_author, m.full_name as member_name, m.email as member_email " +
                     "FROM reservations r " +
                     "JOIN books b ON r.book_id = b.id " +
                     "JOIN members m ON r.member_id = m.id " +
                     "WHERE r.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReservation(rs);
                }
            }
        }
        return null;
    }

    public List<Reservation> findByMemberId(int memberId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, b.title as book_title, b.author as book_author, m.full_name as member_name, m.email as member_email " +
                     "FROM reservations r " +
                     "JOIN books b ON r.book_id = b.id " +
                     "JOIN members m ON r.member_id = m.id " +
                     "WHERE r.member_id = ? " +
                     "ORDER BY r.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
            }
        }
        return list;
    }

    public boolean hasActiveReservation(int memberId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE member_id = ? AND book_id = ? AND status = 'WAITING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            pstmt.setInt(2, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public int insert(Connection conn, Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservations (book_id, member_id, reservation_date, queue_position, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, reservation.getBookId());
            pstmt.setInt(2, reservation.getMemberId());
            pstmt.setString(3, reservation.getReservationDate());
            pstmt.setInt(4, reservation.getQueuePosition());
            pstmt.setString(5, reservation.getStatus());
            pstmt.setString(6, reservation.getCreatedAt());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int genId = keys.getInt(1);
                    reservation.setId(genId);
                    return genId;
                }
            }
        }
        return -1;
    }

    public boolean updateStatus(Connection conn, int reservationId, String status) throws SQLException {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, reservationId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean updateQueuePosition(Connection conn, int reservationId, int newPosition) throws SQLException {
        String sql = "UPDATE reservations SET queue_position = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newPosition);
            pstmt.setInt(2, reservationId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public void recomputeQueuePositions(Connection conn, int bookId) throws SQLException {
        List<Integer> activeIds = new ArrayList<>();
        String selectSql = "SELECT id FROM reservations WHERE book_id = ? AND status = 'WAITING' ORDER BY queue_position ASC, reservation_date ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activeIds.add(rs.getInt("id"));
                }
            }
        }

        String updateSql = "UPDATE reservations SET queue_position = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            for (int i = 0; i < activeIds.size(); i++) {
                pstmt.setInt(1, i + 1);
                pstmt.setInt(2, activeIds.get(i));
                pstmt.executeUpdate();
            }
        }
    }

    public int countActiveReservations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations WHERE status = 'WAITING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation(
                rs.getInt("id"),
                rs.getInt("book_id"),
                rs.getInt("member_id"),
                rs.getString("reservation_date"),
                rs.getInt("queue_position"),
                rs.getString("status")
        );
        r.setCreatedAt(rs.getString("created_at"));
        r.setBookTitle(rs.getString("book_title"));
        r.setBookAuthor(rs.getString("book_author"));
        r.setMemberName(rs.getString("member_name"));
        r.setMemberEmail(rs.getString("member_email"));
        return r;
    }
}
