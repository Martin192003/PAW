package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubDao {
    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone);

    boolean delete(UUID clubId);

    Optional<Club> findById(UUID clubId);

    Optional<Club> findByName(String clubName);

    List<Club> findAll();

    Integer incrementQuantityRatings(UUID clubId, int delta);

    List<Club> search(String nameLike, Zone location, String musicGenre, String priceRange, BigDecimal minRating, BigDecimal maxRating);

    List<Club> findByUserId(UUID userId);

    List<User> findAdminsByClubId(UUID clubId);
}