package ar.edu.itba.paw.model.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException() { }
    public ValidationException(String message) { super(message); }
    public ValidationException(String message, Throwable cause) { super(message, cause); }
}