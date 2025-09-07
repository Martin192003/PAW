package ar.edu.itba.paw.model;

public enum Zone {
    ZONA_NORTE("ZONA_NORTE"),
    ZONA_OESTE("ZONA_OESTE"),
    ZONA_SUR("ZONA_SUR"),
    ZONA_CABA("ZONA_CABA");

    final String zoneName;
    Zone(String zoneName) {
        this.zoneName = zoneName;
    }

    @Override
    public String toString() {
        return zoneName;
    }
}