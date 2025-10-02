package ar.edu.itba.paw.model;

import java.util.Locale;

public enum Status {
    PENDIENTE("pendiente"),
    DISPONIBLE("disponible"),
    CONFIRMADA("confirmada"),
    CANCELADA("cancelada");

    private final String dbValue;

    Status(String dbValue) {
        this.dbValue = dbValue;
    }

    public String db() {
        return dbValue;
    }

    public static Status fromDb(String raw) {
        if (raw == null) return null;
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "pendiente"  -> PENDIENTE;
            case "disponible" -> DISPONIBLE;
            case "confirmada"  -> CONFIRMADA;
            case "cancelada" -> CANCELADA;
            default -> throw new IllegalArgumentException("Unknown status: " + raw);
        };
    }
}