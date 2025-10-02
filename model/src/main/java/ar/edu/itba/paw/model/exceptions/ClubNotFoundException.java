package ar.edu.itba.paw.model.exceptions;

public class ClubNotFoundException extends RuntimeException {
    public ClubNotFoundException() { }
    public ClubNotFoundException(String message) { super(message); }
    public ClubNotFoundException(String message, Throwable cause) { super(message, cause); }
}
