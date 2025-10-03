package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    private final EmailServiceImpl mailService;
    private final ClubTablesDao tablesDao;
    @Autowired
    public ClubServiceImpl(ClubDao clubDao, EmailServiceImpl mailService, ClubTablesDao tablesDao) {
        this.tablesDao = tablesDao;
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
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar) {
        Club newClub = clubDao.create(name, description, location, priceRange, contactInfo, musicGenre, zone, isBar);
        mailService.sendClubCreationConfirmation(contactInfo.getEmail(), name, newClub.getClubId());
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
                                     Boolean isBar,
                                     String sort) {
        return clubDao.search(nameLike, location, musicGenre, priceRange, isBar, sort);
    }

    @Override
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar) {
        return clubDao.update(clubId, name, location, rating, quantityRatings, priceRange, description, contactInfoId, musicGenre, zone, isBar);
    }

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
                club.getZone(),
                club.getIsBar()
        );
    }

    @Override
    public List<Club> getClubsByUser(UUID userId) {
        return clubDao.findByUserId(userId);
    }

    @Override
    public List<User> getAdminsByClub(UUID clubId) {
        return clubDao.findAdminsByClubId(clubId);
    }

    public List<Club> getAllClubsPaginated(int limit, int offset) {
        return clubDao.findAllPaginated(limit, offset);
    }
    public int countAllClubs() {
        return clubDao.countAll();
    }
    public List<Club> getClubsByUserPaginated(UUID userId, int limit, int offset) {
        return clubDao.findByUserIdPaginated(userId, limit, offset);
    }
    public int countClubsByUser(UUID userId) {
        return clubDao.countByUserId(userId);
    }
    @Transactional(readOnly = true)
    public List<Club> searchPaginated(String nameLike,
                                      Zone location,
                                      String musicGenre,
                                      String priceRange,
                                      Boolean isBar,
                                      String sort,
                                      int limit,
                                      int offset) {
        return clubDao.searchPaginated(nameLike, location, musicGenre, priceRange, isBar, sort, limit, offset);
    }

    @Transactional(readOnly = true)
    public int countSearch(String nameLike,
                           Zone location,
                           String musicGenre,
                           String priceRange,
                           Boolean isBar) {
        return clubDao.countSearch(nameLike, location, musicGenre, priceRange, isBar);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<UUID, String> mapClubNames(List<Booking> bookings) {
        var out = new HashMap<UUID, String>(Math.max(16, bookings.size()*2));
        for (var b : bookings) {
            tablesDao.findById(b.getTableId()).ifPresent(t ->
                    clubDao.findById(t.getClubId()).ifPresent(c ->
                            out.put(b.getBookingId(), c.getClubName())
                    )
            );
        }
        return out;
    }
}
