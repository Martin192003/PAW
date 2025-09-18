package ar.edu.itba.paw.model.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() { }
    public UnauthorizedException(String message) { super(message); }
    public UnauthorizedException(String message, Throwable cause) { super(message, cause); }
}