package ar.edu.itba.paw.model.exceptions;

public class TableAlreadyBookedException extends RuntimeException {
    public TableAlreadyBookedException() {
        super("La mesa ya está reservada en el rango indicado");
    }
}