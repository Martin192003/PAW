package ar.edu.itba.paw.model;

public enum Price {
    BAJO("Bajo"),
    MEDIO("Medio"),
    ALTO("Alto");

    private final String label;

    Price(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
