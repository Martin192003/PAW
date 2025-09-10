package ar.edu.itba.paw.model;

public enum Zone {
    ZONA_NORTE("Norte"),
    ZONA_OESTE("Oeste"),
    ZONA_SUR("Sur"),
    ZONA_CABA("CABA");

    private final String label;

    Zone(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}