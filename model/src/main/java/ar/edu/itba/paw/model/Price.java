package ar.edu.itba.paw.model;

public enum Price {
    BAJO("bajo"),
    MEDIO("medio"),
    ALTO("alto");

    private final String label;

    Price(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
