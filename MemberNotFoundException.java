package exceptions;

public class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(String message) {
        super(message, 404);
    }

    public MemberNotFoundException(int memberId) {
        super("Member with ID " + memberId + " was not found.", 404);
    }
}
