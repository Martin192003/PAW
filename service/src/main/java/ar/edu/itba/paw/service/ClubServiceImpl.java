package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    private final EmailServiceImpl mailService;
    private final ClubTablesDao tablesDao;
    private final PhotoService photoService;

    @Autowired
    public ClubServiceImpl(ClubDao clubDao, EmailServiceImpl mailService, ClubTablesDao tablesDao, PhotoService photoService) {
        this.tablesDao = tablesDao;
        this.mailService = mailService;
        this.clubDao = clubDao;
        this.photoService = photoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Club> getAllClubs() {
        return clubDao.findAll();
    }

    @Override
    @Transactional
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar, MultipartFile image) {
        Club newClub = clubDao.create(name, description, location, priceRange, contactInfo, musicGenre, zone, isBar);

        if (image != null && !image.isEmpty()) {
            try {
                photoService.upload(newClub.getClubId(), contactInfo.getId(),
                        image.getOriginalFilename(), image.getContentType(), image.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read uploaded image", e);
            }
        }

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
    @Transactional
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar) {
        return clubDao.update(clubId, name, location, rating, quantityRatings, priceRange, description, contactInfoId, musicGenre, zone, isBar);
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
                club.getIsBar()
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
    public List<Club> getAllClubsPaginated(int limit, int offset) {
        return clubDao.findAllPaginated(limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public int countAllClubs() {
        return clubDao.countAll();
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
    @Transactional
    public void updateField(UUID clubId, String field, String value, UUID editorId){
        assertAdmin(clubId, editorId);

        Club club = clubDao.findById(clubId).orElseThrow(ClubNotFoundException::new);

        switch (field) {
            case "clubName":
                club.setClubName(value);
                break;
            case "location":
                club.setLocation(value);
                break;
            case "priceRange":
                club.setPriceRange(Price.valueOf(value.toUpperCase(Locale.ROOT)));
                break;
            case "description":
                club.setDescription(value);
                break;
            case "zone":
                club.setZone(parseZoneFlexible(value)); // <-- acepta name() o dbValue
                break;
            case "musicGenre":
                club.setMusicGenre(MusicalGenre.valueOf(value));
                break;
            default:
                throw new IllegalArgumentException("Campo inválido: " + field);
        }

        // IMPORTANTÍSIMO: persistí la instancia MODIFICADA
        updateClub(club);
    }

    @Override
    public boolean sendContactMessage(UUID clubId, String senderEmail, String senderName, String senderPhone, String message) {
        Optional<Club> clubOpt = clubDao.findById(clubId);
        if (clubOpt.isEmpty()) {
            return false;
        }

        Club club = clubOpt.get();
        String recipientEmail = club.getContactInfo() != null ? club.getContactInfo().getEmail() : "test@local";

        return mailService.sendContact(
                recipientEmail,
                club.getClubName(),
                senderEmail,
                senderName,
                senderPhone,
                message
        );
    }

    private Zone parseZoneFlexible(String raw) {
        if (raw == null) throw new IllegalArgumentException("Zona vacía");
        // 1) Intento por name() del enum (con normalización básica)
        String normalized = normalizeEnumToken(raw);
        try {
            return Zone.valueOf(normalized);
        } catch (IllegalArgumentException ignore) {
            // 2) Fallback por dbValue (lo que envía el JSP)
            for (Zone z : Zone.values()) {
                // si tu enum tiene getDbValue(); si no, reemplazá por el método/prop correcto
                String db = z.getDbValue();
                if (db != null && db.equalsIgnoreCase(raw)) {
                    return z;
                }
            }
            // 3) No hubo match
            throw new IllegalArgumentException("Zona inválida: " + raw);
        }
    }

    private String normalizeEnumToken(String v) {
        String s = v.trim().replace('-', '_').replace(' ', '_');
        // si querés tolerar acentos:
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toUpperCase(Locale.ROOT);
    }

}
