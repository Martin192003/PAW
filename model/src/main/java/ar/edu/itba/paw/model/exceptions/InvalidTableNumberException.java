package ar.edu.itba.paw.model.exceptions;

public class InvalidTableNumberException extends RuntimeException {
    public InvalidTableNumberException(String message) {
        super(message);
    }
}
