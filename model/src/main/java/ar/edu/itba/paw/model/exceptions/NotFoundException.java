package ar.edu.itba.paw.model.exceptions;

public abstract class NotFoundException extends RuntimeException {
    public NotFoundException() {}
    public NotFoundException(Throwable message) {
        super(message);
    }
}
