package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Zone;
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
}
