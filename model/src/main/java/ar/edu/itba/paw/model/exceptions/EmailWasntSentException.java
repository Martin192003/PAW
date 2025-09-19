package ar.edu.itba.paw.model.exceptions;

public class EmailWasntSentException extends RuntimeException{
    public EmailWasntSentException(String message) {
        super(message);
    }
}
