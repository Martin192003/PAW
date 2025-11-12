package ar.edu.itba.paw.model.exceptions;

import java.util.UUID;

public class InvalidPriceException extends ValidationException {
    private final UUID clubId;
    private final UUID productId;
    public InvalidPriceException(UUID clubId, UUID productId, String message) {
        super(message);
        this.clubId = clubId;
        this.productId = productId;
    }
    public InvalidPriceException(UUID clubId, String message) {
        super(message);
        this.clubId = clubId;
        this.productId = null;
    }
    public InvalidPriceException(String message) {
        super(message);
        this.clubId = null;
        this.productId = null;
    }
    public UUID getClubId() { return clubId; }
    public UUID getProductId() { return productId; }
}