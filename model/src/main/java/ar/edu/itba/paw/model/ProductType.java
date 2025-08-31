package ar.edu.itba.paw.model;

import java.util.Locale;

public enum ProductType {
    ALCOHOLICA,
    NO_ALCOHOLICA,
    COMIDA;

    public static ProductType fromDb(String raw) {
        if (raw == null) return null;

        // normalizamos: pasamos todo a mayúsculas y agregamos _
        String normalized = raw
                .replaceAll("([a-z])([A-Z])", "$1_$2") // noAlcoholicas -> no_Alcoholicas
                .toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ALCOHOLICAS", "ALCOHOLICA" -> ALCOHOLICA;
            case "NO_ALCOHOLICAS", "NO_ALCOHOLICA" -> NO_ALCOHOLICA;
            case "COMIDA", "COMIDAS" -> COMIDA;
            default -> throw new IllegalArgumentException("Unknown product_type: " + raw);
        };
    }
}
