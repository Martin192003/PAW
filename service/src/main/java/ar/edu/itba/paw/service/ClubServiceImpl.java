package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.ClubDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    private final MailService mailService;
    @Autowired
    public ClubServiceImpl(ClubDao clubDao, MailService mailService) {
        this.mailService = mailService;
        this.clubDao = clubDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubDao.findAll();
    }

    @Override
    @Transactional
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone){
        Club newClub = clubDao.create(name, description, location, priceRange, contactInfo, musicGenre, zone);
        mailService.sendClubCreationConfirmation(contactInfo.getEmail(), name);
        return newClub;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Club> getClubById(UUID clubId) {
        return clubDao.findById(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> advancedSearch(String nameLike,
                                     Zone location,
                                     String musicGenre,
                                     String priceRange,
                                     BigDecimal minRating,
                                     BigDecimal maxRating) {
        return clubDao.search(nameLike, location, musicGenre, priceRange, minRating, maxRating);
    }
}
