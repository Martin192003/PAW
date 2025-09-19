package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.ClubDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    @Autowired
    public ClubServiceImpl(ClubDao clubDao) {
        this.clubDao = clubDao;
    }

    @Override
    public List<Club> getAllClubs() {
        return clubDao.findAll();
    }

    @Override
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone){
        return clubDao.create(name, description, location, priceRange, contactInfo, musicGenre, zone);
    }

    @Override
    public Optional<Club> getClubById(UUID clubId) {
        return clubDao.findById(clubId);
    }

    @Override
    public List<Club> advancedSearch(String nameLike,
                                     Zone location,
                                     String musicGenre,
                                     String priceRange,
                                     BigDecimal minRating,
                                     BigDecimal maxRating) {
        return clubDao.search(nameLike, location, musicGenre, priceRange, minRating, maxRating);
    }

    @Override
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone) {
        return clubDao.update(clubId, name, location, rating, quantityRatings, priceRange, description, contactInfoId, musicGenre, zone);
    }

    // ClubService.java
    public Club updateClub(Club club) {
        return clubDao.update(
                club.getClubId(),
                club.getClubName(),
                club.getLocation(),
                club.getRating(),
                club.getQuantityRatings(),
                club.getPriceRange(),
                club.getDescription(),
                club.getContactInfo() != null ? club.getContactInfo().getId() : null,
                club.getMusicGenre(),
                club.getZone()
        );
    }

    @Override
    public List<Club> getClubsByUser(UUID userId) {
        return clubDao.findByUserId(userId);
    }


}
