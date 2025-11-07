package ar.edu.itba.paw.model.exceptions;

public class ReviewNotFounException extends RuntimeException {
    public ReviewNotFounException(String message) {
        super(message);
    }
}
