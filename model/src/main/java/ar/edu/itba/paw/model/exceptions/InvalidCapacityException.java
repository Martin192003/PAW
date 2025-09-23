package ar.edu.itba.paw.model.exceptions;

public class InvalidCapacityException extends RuntimeException {
    public InvalidCapacityException() { }
    public InvalidCapacityException(String message) { super(message); }
    public InvalidCapacityException(String message, Throwable cause) { super(message, cause); }
}
