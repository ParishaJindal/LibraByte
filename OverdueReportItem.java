package model;

/**
 * Report DTO for Overdue Books.
 */
public class OverdueReportItem {
    private int issueId;
    private int memberId;
    private String memberName;
    private String memberEmail;
    private int bookId;
    private String bookTitle;
    private String issueDate;
    private String dueDate;
    private long overdueDays;
    private String status;

    public OverdueReportItem(int issueId, int memberId, String memberName, String memberEmail,
                             int bookId, String bookTitle, String issueDate, String dueDate,
                             long overdueDays, String status) {
        this.issueId = issueId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.overdueDays = overdueDays;
        this.status = status;
    }

    public int getIssueId() { return issueId; }
    public int getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getMemberEmail() { return memberEmail; }
    public int getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getIssueDate() { return issueDate; }
    public String getDueDate() { return dueDate; }
    public long getOverdueDays() { return overdueDays; }
    public String getStatus() { return status; }
}
