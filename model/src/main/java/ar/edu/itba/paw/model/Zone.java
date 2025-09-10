package ar.edu.itba.paw.model;

public enum Zone {
    ZONA_NORTE("Zona Norte"),
    ZONA_OESTE("Zona Oeste"),
    ZONA_SUR("Zona Sur"),
    ZONA_CABA("CABA");

    final String zoneName;
    Zone(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getDisplayName() {
        return zoneName;
    }

    public String getDbValue() {
        return name();
    }

    @Override
    public String toString() {
        return zoneName;
    }
}