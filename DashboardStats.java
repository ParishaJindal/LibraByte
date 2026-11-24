package model;

import java.util.List;

/**
 * DTO for the executive dashboard overview.
 */
public class DashboardStats {
    private int totalMembers;
    private int totalBooks;
    private int totalCopies;
    private int availableCopies;
    private int issuedBooks;
    private int activeReservations;
    private int overdueBooks;
    private List<IssueRecord> recentIssues;
    private List<IssueRecord> recentReturns;
    private List<PopularBookReportItem> popularBooks;

    public DashboardStats(int totalMembers, int totalBooks, int totalCopies, int availableCopies,
                          int issuedBooks, int activeReservations, int overdueBooks,
                          List<IssueRecord> recentIssues, List<IssueRecord> recentReturns,
                          List<PopularBookReportItem> popularBooks) {
        this.totalMembers = totalMembers;
        this.totalBooks = totalBooks;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.issuedBooks = issuedBooks;
        this.activeReservations = activeReservations;
        this.overdueBooks = overdueBooks;
        this.recentIssues = recentIssues;
        this.recentReturns = recentReturns;
        this.popularBooks = popularBooks;
    }

    public int getTotalMembers() { return totalMembers; }
    public int getTotalBooks() { return totalBooks; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }
    public int getIssuedBooks() { return issuedBooks; }
    public int getActiveReservations() { return activeReservations; }
    public int getOverdueBooks() { return overdueBooks; }
    public List<IssueRecord> getRecentIssues() { return recentIssues; }
    public List<IssueRecord> getRecentReturns() { return recentReturns; }
    public List<PopularBookReportItem> getPopularBooks() { return popularBooks; }
}
