package ar.edu.itba.paw.model.exceptions;


import ar.edu.itba.paw.model.Status;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Status from, Status to) {
        super("Transición de estado inválida: " + from + " → " + to);
    }
}