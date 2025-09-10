package ar.edu.itba.paw.model;


public enum Zone {
    ZONA_NORTE("Zona Norte"),
    ZONA_OESTE("Zona Oeste"),
    ZONA_SUR("Zona Sur"),
    ZONA_CABA("CABA");

    private final String label;

    Zone(String zoneName) { this.label = zoneName; }

    public String getDisplayName() { return label; }
    public String getDbValue() { return name(); }

    @Override public String toString() { return label; }
}