package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.club.Boliche;
import ar.edu.itba.paw.model.club.MusicalGenre;
import ar.edu.itba.paw.model.club.Price;
import ar.edu.itba.paw.model.club.VenueType;

import java.util.Optional;
import java.util.Set;

public interface BolicheService {

    Optional<Boliche> findById(Long id);
    Optional<Boliche> findBySlug(String slug);

  //  Optional<Boliche> search(BolicheFilter filter, BolicheSort sort, int page, int pageSize); //PREGUNTAR DONDE VA BolicheFilter EN SERVICE? EN MODEL?


    boolean existsBySlug(String slug);
  //  long count(BolicheFilter filter);

    Boliche create(String name, String address, String city, String province,
                   String description, VenueType venueType, Price price,
                   Set<MusicalGenre> musicalGenres);

    void updateBoliche(Boliche boliche, String name, String address, String city, String province,
                       String description, Price price, Set<MusicalGenre> musicalGenres);

    void registerRating(Long bolicheId, int stars);
}