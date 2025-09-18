package ar.edu.itba.paw.model.exceptions;

public class BookingNotFoundException extends  RuntimeException {
    public BookingNotFoundException() { }
    public BookingNotFoundException(String message) { super(message); }
    public BookingNotFoundException(String message, Throwable cause) { super(message, cause); }
}
