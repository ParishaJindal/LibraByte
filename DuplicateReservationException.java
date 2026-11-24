package exceptions;

public class DuplicateReservationException extends LibraryException {
    public DuplicateReservationException(String message) {
        super(message, 409);
    }

    public DuplicateReservationException(int memberId, int bookId) {
        super("Member ID " + memberId + " already has an active reservation for Book ID " + bookId + ".", 409);
    }
}
