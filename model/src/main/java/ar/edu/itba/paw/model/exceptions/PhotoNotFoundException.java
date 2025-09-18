package ar.edu.itba.paw.model.exceptions;

public class PhotoNotFoundException extends  RuntimeException {
    public PhotoNotFoundException() { }
    public PhotoNotFoundException(String message) { super(message); }
    public PhotoNotFoundException(String message, Throwable cause) { super(message, cause); }
}
