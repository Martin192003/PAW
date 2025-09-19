package ar.edu.itba.paw.model.exceptions;

public class InvalidBookingStateException extends RuntimeException{
    public InvalidBookingStateException() { }
    public InvalidBookingStateException(String message) { super(message); }
    public InvalidBookingStateException(String message, Throwable cause) { super(message, cause); }
}
