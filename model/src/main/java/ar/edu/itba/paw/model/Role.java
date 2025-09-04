package ar.edu.itba.paw.model;

public enum Role {
    USER,
    ADMIN;

   @Override
    public String toString() {
         return name().toLowerCase();
    }
}
