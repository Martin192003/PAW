package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubDao {
    Club create(String name, String description, String location, String priceRange, User contactInfo, String musicGenre);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, String priceRange, String description, UUID contactInfoId, String musicGenre);

    boolean delete(UUID clubId);
    Optional<Club> findById(UUID clubId);
    Optional<Club> findByName(String clubName);
    List<Club> findAll();
    Integer incrementQuantityRatings(UUID clubId, int delta);
}
