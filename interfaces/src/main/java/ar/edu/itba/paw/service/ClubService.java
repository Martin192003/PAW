package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();

    Optional<Club> getClubById(UUID clubId);

    List<Club> advancedSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort);

    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar);

    Club updateClub(Club club);

    List<Club> getClubsByUser(UUID userId);

    List<User> getAdminsByClub(UUID clubId);
}