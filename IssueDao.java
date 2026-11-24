package dao;

import database.DatabaseConnection;
import model.IssueRecord;
import model.OverdueReportItem;
import util.DateUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueDao {

    public List<IssueRecord> findAll() throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, b.title as book_title, b.isbn as book_isbn, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "ORDER BY ir.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToIssue(rs));
            }
        }
        return list;
    }

    public IssueRecord findById(int id) throws SQLException {
        String sql = "SELECT ir.*, b.title as book_title, b.isbn as book_isbn, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "WHERE ir.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToIssue(rs);
                }
            }
        }
        return null;
    }

    public List<IssueRecord> findByMemberId(int memberId) throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, b.title as book_title, b.isbn as book_isbn, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "WHERE ir.member_id = ? " +
                     "ORDER BY ir.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIssue(rs));
                }
            }
        }
        return list;
    }

    public int insert(Connection conn, IssueRecord issue) throws SQLException {
        String sql = "INSERT INTO issue_records (book_id, member_id, issue_date, due_date, return_date, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, issue.getBookId());
            pstmt.setInt(2, issue.getMemberId());
            pstmt.setString(3, issue.getIssueDate());
            pstmt.setString(4, issue.getDueDate());
            pstmt.setString(5, issue.getReturnDate());
            pstmt.setString(6, issue.getStatus());
            pstmt.setString(7, issue.getCreatedAt());
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int genId = keys.getInt(1);
                    issue.setId(genId);
                    return genId;
                }
            }
        }
        return -1;
    }

    public boolean updateReturn(Connection conn, int issueId, String returnDate, String status) throws SQLException {
        String sql = "UPDATE issue_records SET return_date = ?, status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnDate);
            pstmt.setString(2, status);
            pstmt.setInt(3, issueId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<IssueRecord> getRecentIssues(int limit) throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, b.title as book_title, b.isbn as book_isbn, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "ORDER BY ir.id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIssue(rs));
                }
            }
        }
        return list;
    }

    public List<IssueRecord> getRecentReturns(int limit) throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, b.title as book_title, b.isbn as book_isbn, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "WHERE ir.status = 'RETURNED' AND ir.return_date IS NOT NULL " +
                     "ORDER BY ir.return_date DESC, ir.id DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIssue(rs));
                }
            }
        }
        return list;
    }

    public List<OverdueReportItem> getOverdueRecords() throws SQLException {
        List<OverdueReportItem> items = new ArrayList<>();
        String today = DateUtils.today();
        String sql = "SELECT ir.id as issue_id, ir.book_id, ir.member_id, ir.issue_date, ir.due_date, ir.status, " +
                     "b.title as book_title, m.full_name as member_name, m.email as member_email " +
                     "FROM issue_records ir " +
                     "JOIN books b ON ir.book_id = b.id " +
                     "JOIN members m ON ir.member_id = m.id " +
                     "WHERE ir.status IN ('ISSUED', 'OVERDUE') AND ir.due_date < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String dueDate = rs.getString("due_date");
                    long overdueDays = DateUtils.calculateOverdueDays(dueDate);
                    items.add(new OverdueReportItem(
                            rs.getInt("issue_id"),
                            rs.getInt("member_id"),
                            rs.getString("member_name"),
                            rs.getString("member_email"),
                            rs.getInt("book_id"),
                            rs.getString("book_title"),
                            rs.getString("issue_date"),
                            dueDate,
                            overdueDays,
                            "OVERDUE"
                    ));
                }
            }
        }
        return items;
    }

    public Map<Integer, Integer> getBorrowCounts() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT book_id, COUNT(*) as borrow_count FROM issue_records GROUP BY book_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("book_id"), rs.getInt("borrow_count"));
            }
        }
        return counts;
    }

    public int countActiveIssues() throws SQLException {
        String sql = "SELECT COUNT(*) FROM issue_records WHERE status IN ('ISSUED', 'OVERDUE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private IssueRecord mapResultSetToIssue(ResultSet rs) throws SQLException {
        IssueRecord issue = new IssueRecord(
                rs.getInt("id"),
                rs.getInt("book_id"),
                rs.getInt("member_id"),
                rs.getString("issue_date"),
                rs.getString("due_date"),
                rs.getString("return_date"),
                rs.getString("status")
        );
        issue.setCreatedAt(rs.getString("created_at"));
        issue.setBookTitle(rs.getString("book_title"));
        issue.setBookIsbn(rs.getString("book_isbn"));
        issue.setMemberName(rs.getString("member_name"));
        issue.setMemberEmail(rs.getString("member_email"));
        return issue;
    }
}
