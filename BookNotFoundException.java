package exceptions;

public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String message) {
        super(message, 404);
    }

    public BookNotFoundException(int bookId) {
        super("Book with ID " + bookId + " was not found in the library catalog.", 404);
    }
}
