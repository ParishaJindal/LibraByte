package exceptions;

public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String message) {
        super(message, 409); // Conflict / Not available
    }

    public BookNotAvailableException(int bookId) {
        super("Book with ID " + bookId + " has no available copies for issuing. Please reserve it.", 409);
    }
}
