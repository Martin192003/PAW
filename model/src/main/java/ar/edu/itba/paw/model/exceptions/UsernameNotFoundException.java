package ar.edu.itba.paw.model.exceptions;

public class UsernameNotFoundException extends NotFoundException {
    public UsernameNotFoundException() {}
    public UsernameNotFoundException(Throwable message) {
        super(message);
    }
}
