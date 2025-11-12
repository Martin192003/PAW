package ar.edu.itba.paw.model.exceptions;

import java.util.UUID;

public class MaxProductsReachedException extends ValidationException {
    private final UUID clubId;
    public MaxProductsReachedException(UUID clubId) { this.clubId = clubId; }
    public UUID getClubId() { return clubId; }
}