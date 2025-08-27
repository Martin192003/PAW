package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.club.Club;
import ar.edu.itba.paw.model.club.MusicalGenre;
import ar.edu.itba.paw.model.club.Price;
import ar.edu.itba.paw.model.club.VenueType;

import java.util.Optional;
import java.util.Set;

public interface ClubService {

    Optional<Club> findById(Long id);
    Optional<Club> findBySlug(String slug);

  //  Optional<Club> search(ClubFilter filter, ClubSort sort, int page, int pageSize); //PREGUNTAR DONDE VA ClubFilter EN SERVICE? EN MODEL?


    boolean existsBySlug(String slug);
  //  long count(ClubFilter filter);

    Club create(String name, String address, String city, String province,
                   String description, VenueType venueType, Price price,
                   Set<MusicalGenre> musicalGenres);

    void updateClub(Club Club, String name, String address, String city, String province,
                       String description, Price price, Set<MusicalGenre> musicalGenres);

    void registerRating(Long ClubId, int stars);
}