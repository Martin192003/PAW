package ar.edu.itba.paw.model;

import java.util.Locale;

public enum TableState {
    DISPONIBLE("disponible"),
    RESERVADA("reservada"),
    PENDIENTE("pendiente");

    private final String dbValue;

    TableState(String state) { this.dbValue = state; }

    public String db() { return dbValue; }

    public static TableState fromDb(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "disponible" -> DISPONIBLE;
            case "reservada"  -> RESERVADA;
            case "pendiente"  -> PENDIENTE;
            default -> throw new IllegalArgumentException("Unknown table_state: " + raw);
        };
    }
}
