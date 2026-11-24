package exceptions;

public class LibraryException extends Exception {
    private final int statusCode;

    public LibraryException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public LibraryException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
