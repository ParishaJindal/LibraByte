package exceptions;

public class InvalidReturnException extends LibraryException {
    public InvalidReturnException(String message) {
        super(message, 400);
    }
}
