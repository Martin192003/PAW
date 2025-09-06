package ar.edu.itba.paw.model;

public enum Zone {
    ZONA_NORTE("Zona norte"),
    ZONA_OESTE("Zona oeste"),
    ZONA_SUR("Zona sur"),
    ZONA_CABA("CABA");

    final String zoneName;
    Zone(String zoneName) {
        this.zoneName = zoneName;
    }

    @Override
    public String toString() {
        return zoneName;
    }
}
