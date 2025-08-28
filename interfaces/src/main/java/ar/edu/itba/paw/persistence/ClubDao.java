package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.UUID;

public interface ClubDao {
    Club findById(UUID clubId);
    Club findByName(String clubName);
    List<Club> findAll();

    Club create(String name, String description, String location, String priceRange, User contactInfo, String musicGenre);
    Club update(Club club);
    boolean delete(UUID clubId);
}
