package ar.edu.itba.paw.model.exceptions;

public class InvalidClubException extends RuntimeException {
    public InvalidClubException() { }
    public InvalidClubException(String message) { super(message); }
    public InvalidClubException(String message, Throwable cause) { super(message, cause); }
}
