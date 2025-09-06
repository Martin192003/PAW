package ar.edu.itba.paw.model;

import java.util.Locale;

public enum ProductType {
    ALCOHOLICA("alcoholicas"),
    NO_ALCOHOLICA("noAlcoholicas"),
    TRAGOS("tragos");

    private final String dbValue;

    ProductType(String dbValue) { this.dbValue = dbValue; }

    public String db() { return dbValue; }

    public static ProductType fromDb(String raw) {
        if (raw == null) return null;

        String normalized = raw
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ALCOHOLICAS", "ALCOHOLICA" -> ALCOHOLICA;
            case "NO_ALCOHOLICAS", "NO_ALCOHOLICA" -> NO_ALCOHOLICA;
            case "TRAGOS" -> TRAGOS;
            default -> throw new IllegalArgumentException("Unknown product_type: " + raw);
        };
    }
}
