package ar.edu.itba.paw.model;

import java.util.Locale;

public enum TableState {
    DISPONIBLE("disponible"),
    CONFIRMADA("confirmada"),
    RESERVADA("reservada");

    private final String dbValue;

    TableState(String state) { this.dbValue = state; }

    public String db() { return dbValue; }

    public static TableState fromDb(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "disponible" -> DISPONIBLE;
            case "confirmada" -> CONFIRMADA;
            case "reservada"  -> RESERVADA;
            default -> throw new IllegalArgumentException("Unknown table_state: " + raw);
        };
    }
}
