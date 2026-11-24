package model;

/**
 * Reservation entity representing a member waiting in the FIFO queue for an unavailable book.
 */
public class Reservation extends BaseEntity implements Comparable<Reservation> {
    private int bookId;
    private int memberId;
    private String reservationDate;
    private int queuePosition;
    private String status; // WAITING, ALLOCATED, CANCELLED, FULFILLED

    // Denormalized fields
    private String bookTitle;
    private String bookAuthor;
    private String memberName;
    private String memberEmail;

    public Reservation() {
        super();
        this.status = "WAITING";
    }

    public Reservation(int id, int bookId, int memberId, String reservationDate, int queuePosition, String status) {
        super(id);
        this.bookId = bookId;
        this.memberId = memberId;
        this.reservationDate = reservationDate;
        this.queuePosition = queuePosition;
        this.status = status;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(String reservationDate) {
        this.reservationDate = reservationDate;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookAuthor() {
        return bookAuthor;
    }

    public void setBookAuthor(String bookAuthor) {
        this.bookAuthor = bookAuthor;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    /**
     * Natural FIFO ordering based on reservation date and queue position.
     */
    @Override
    public int compareTo(Reservation other) {
        if (other == null) return 1;
        int dateCmp = this.reservationDate.compareTo(other.reservationDate);
        if (dateCmp != 0) return dateCmp;
        return Integer.compare(this.queuePosition, other.queuePosition);
    }

    @Override
    public String toString() {
        return "Reservation[id=" + id + ", book=" + bookId + ", member=" + memberId + 
               ", pos=" + queuePosition + ", status=" + status + ", date=" + reservationDate + "]";
    }
}
