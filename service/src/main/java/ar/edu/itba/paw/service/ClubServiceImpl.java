package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    private final EmailService mailService;
    private final ClubTablesDao tablesDao;
    private final PhotoService photoService;
    private final UserService userService;

    @Autowired
    public ClubServiceImpl(ClubDao clubDao, EmailService mailService, ClubTablesDao tablesDao, PhotoService photoService, UserService userService) {
        this.tablesDao = tablesDao;
        this.mailService = mailService;
        this.clubDao = clubDao;
        this.photoService = photoService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public Club create(String name, String description, String location, Price priceRange,
                       User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu, MultipartFile image) {

        User managedContact = userService.findById(contactInfo.getId());
        if (managedContact.getRole() != Role.ADMIN) {
            managedContact.setRole(Role.ADMIN);
        }

        Club newClub = clubDao.create(name, description, location, priceRange, managedContact, musicGenre, zone, isBar, cbu);

        if (image != null && !image.isEmpty()) {
            try {
                photoService.uploadCover(
                        newClub.getClubId(),
                        managedContact.getId(),
                        image.getOriginalFilename(),
                        image.getContentType(),
                        image.getBytes()
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to read uploaded image", e);
            }
        }

        mailService.sendClubCreationConfirmation(managedContact.getEmail(), name, newClub.getClubId());

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
    @Transactional
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu) {
        return clubDao.update(clubId, name, location, rating, quantityRatings, priceRange, description, contactInfoId, musicGenre, zone, isBar, cbu);
    }

    @Override
    @Transactional
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
                club.getIsBar(),
                club.getCbu()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByUser(UUID userId) {
        return clubDao.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAdminsByClub(UUID clubId) {
        return clubDao.findAdminsByClubId(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsByUserPaginated(UUID userId, int limit, int offset) {
        return clubDao.findByUserIdPaginated(userId, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countClubsByUser(UUID userId) {
        return clubDao.countByUserId(userId);
    }

    @Override
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

    @Override
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

    @Override
    public void assertAdmin(UUID clubId, UUID userId) {
        List<User> admins = clubDao.findAdminsByClubId(clubId);
        admins.stream().filter(a -> a.getId().equals(userId)).findAny().orElseThrow(UnauthorizedException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean sendContactMessage(UUID clubId, String senderEmail, String senderName, String senderPhone, String message) {
        Optional<Club> clubOpt = clubDao.findById(clubId);
        if (clubOpt.isEmpty()) return false;

        Club club = clubOpt.get();

        String recipientEmail = (club.getContactInfo() != null && club.getContactInfo().getEmail() != null)
                ? club.getContactInfo().getEmail()
                : fallbackClubEmail(club);

        mailService.sendContact(
                recipientEmail,
                club.getClubName(),
                senderEmail,
                senderName,
                senderPhone,
                message
        );
        return true;
    }

    private String fallbackClubEmail(Club club) {
        if (club.getContactInfo() != null && club.getContactInfo().getEmail() != null) {
            return club.getContactInfo().getEmail();
        }
        return "paw2025b06@gmail.com";
    }

    private Zone parseZoneFlexible(String raw) {
        if (raw == null) throw new IllegalArgumentException("Zona vacía");
        String normalized = normalizeEnumToken(raw);
        try {
            return Zone.valueOf(normalized);
        } catch (IllegalArgumentException ignore) {
            for (Zone z : Zone.values()) {
                String db = z.getDbValue();
                if (db != null && db.equalsIgnoreCase(raw)) {
                    return z;
                }
            }
            throw new IllegalArgumentException("Zona inválida: " + raw);
        }
    }

    private String normalizeEnumToken(String v) {
        String s = v.trim().replace('-', '_').replace(' ', '_');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getClubsWithBookingsByUser(UUID userId) {
        return clubDao.findClubsWithBookingsByUserId(userId);
    }



    @Override
    @Transactional(readOnly = true)
    public List<Club> searchAvailablePaginated(String nameLike, Zone location, String musicGenre,
                                               String priceRange, Boolean isBar, String sort,
                                               LocalDate day, int limit, int offset) {
        return clubDao.searchAvailablePaginated(nameLike, location, musicGenre, priceRange, isBar, sort, day, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchAvailable(String nameLike, Zone location, String musicGenre,
                                    String priceRange, Boolean isBar, LocalDate day) {
        return clubDao.countSearchAvailable(nameLike, location, musicGenre, priceRange, isBar, day);
    }
}
