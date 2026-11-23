package exception;

public class BookingConflictException extends RuntimeException {
    public BookingConflictException() {
        super("Resource is already booked in this time range");
    }
}
