package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Zone;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();
    Optional<Club> getClubById(UUID clubId);
    List<Club> advancedSearch(String nameLike, Zone location, String musicGenre, String priceRange, BigDecimal minRating, BigDecimal maxRating);

}