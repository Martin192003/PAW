package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();
    Optional<Club> getClubById(UUID clubId);
    List<Club> advancedSearch(String nameLike, Zone location, String musicGenre, String priceRange, BigDecimal minRating, BigDecimal maxRating);

    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone);
}