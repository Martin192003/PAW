package ar.edu.itba.paw.model.exceptions;

import java.util.UUID;

public class TableNotFoundException extends RuntimeException {
    public TableNotFoundException(UUID tableId) {
        super("No existe la mesa con id: " + tableId);
    }
}