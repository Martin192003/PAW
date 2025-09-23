package ar.edu.itba.paw.model;

public enum MusicalGenre {
    ROCK("Rock"),
    POP("Pop"),
    CACHENGUE("Cachengue"),
    TECHNO("Techno"),
    REGGAETON("Reggaetón"),
    CUMBIA("Cumbia"),
    TRAP("Trap"),
    JAZZ("Jazz"),
    ELECTRONICA("Electrónica"),
    AFROBEAT("Afrobeat"),
    OTRO("Otro");

    private final String label;

    MusicalGenre(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
