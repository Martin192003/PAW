package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.dto.ClubLimitsDto;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ClubServiceImpl implements ClubService{


    private static final int DESCRIPTION_MAX_LENGTH = 1000;

    private final ClubDao clubDao;
    private final ClubTablesDao clubTablesDao;
    private final EmailService mailService;
    private final PhotoService photoService;
    private final UserService userService;
    private final BookingDao bookingDao;

    @Value("${club.contact.fallback-email:no-reply@local}")
    private String fallbackContactEmail;

    @Autowired
    public ClubServiceImpl(ClubDao clubDao, ClubTablesDao clubTablesDao, EmailService mailService, PhotoService photoService, UserService userService, BookingDao bookingDao) {
        this.clubTablesDao = clubTablesDao;
        this.mailService = mailService;
        this.clubDao = clubDao;
        this.photoService = photoService;
        this.userService = userService;
        this.bookingDao = bookingDao;
    }

    @Override
    @Transactional
    public Club create(String name, String description, String location, Price priceRange,
                       User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu, Photo image) {

        validateDescription(description);

        User managedContact = userService.findById(contactInfo.getId()).orElseThrow();
        if (managedContact.getRole() != Role.ADMIN) {
            managedContact.setRole(Role.ADMIN);
            userService.updateRole(managedContact.getId(), Role.ADMIN);
        }

        Club newClub = clubDao.create(name, description, location, priceRange, managedContact, musicGenre, zone, isBar, cbu);

        if (image != null) {
                photoService.uploadCover(
                        newClub.getClubId(),
                        managedContact.getId(),
                        image.getFilename(),
                        image.getContentType(),
                        image.getData()
                );
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
    @Transactional
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu) {
        validateDescription(description);
        return clubDao.update(clubId, name, location, rating, quantityRatings, priceRange, description, contactInfoId, musicGenre, zone, isBar, cbu);
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new ValidationException("description too long (max " + DESCRIPTION_MAX_LENGTH + ")");
        }
    }

    @Override
    @Transactional
    public void updateClub(Club club) {
        clubDao.update(
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
    public List<UUID> findManagedClubIds(UUID userId) {
        return clubDao.findIdsByAdmin(userId);
    }

    @Override
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
        List<UUID> bookingIds = bookings.stream()
                .map(Booking::getBookingId)
                .toList();

        return bookingDao.mapClubNamesForBookings(bookingIds);
    }

    @Override
    @Transactional(readOnly = true)
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
        return fallbackContactEmail;
    }

    @Override
    @Transactional(readOnly = true)
    public Club getClubByIdOrThrow(UUID clubId) {
        return clubDao.findById(clubId)
                .orElseThrow(() -> new NotFoundException("error.club.notfound"));
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
    public int countPagesForUser(UUID userId, int size) {
        if (size <= 0) {
            return 1;
        }

        int totalClubs = countClubsByUser(userId);
        return Math.max(1, (int) Math.ceil((double) totalClubs / size));
    }

    @Override
    @Transactional(readOnly = true)
    public int countSearchAvailable(String nameLike, Zone location, String musicGenre,
                                    String priceRange, Boolean isBar, LocalDate day) {
        return clubDao.countSearchAvailable(nameLike, location, musicGenre, priceRange, isBar, day);
    }

    public boolean isUserOwner(UUID clubId, User user) {
        if (user == null) {
            return false;
        }
        if (!user.getRole().equals(Role.ADMIN)) {
            return false;
        }
        return getAdminsByClub(clubId)
                .stream()
                .anyMatch(u -> u.getId().equals(user.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public int normalizePage(int page, UUID userId, int size) {
        int totalPages=countPagesForUser(userId, size);
        if ( page <= totalPages)
            return 1;
        return totalPages;
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateOffset(int page, UUID userId,  int size) {
        page= normalizePage(page, userId, size);
        return (page - 1) * size;
    }

    @Override
    @Transactional(readOnly = true)
    public int pagesForCount(int size,String nameLike,Zone zone,String musicGenre,String normalizedPriceRange,Boolean isBar) {
        int totalClubs = countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        return Math.max(1, (int) Math.ceil((double) totalClubs / size));
    }

    @Override
    @Transactional(readOnly = true)
    public int offsetForPage(int page, int size, String nameLike,Zone zone,String musicGenre,String normalizedPriceRange,Boolean isBar) {
        if (size <= 0) throw new IllegalArgumentException("size must be > 0");
        int totalPages = pagesForCount(size, nameLike,zone,musicGenre,normalizedPriceRange,isBar);
        int p = Math.max(1, Math.min(page, totalPages));
        return (p - 1) * size;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Club> getClubByBookingId(UUID bookingId){
        Booking booking = bookingDao.findById(bookingId).orElseThrow(NotFoundException::new);
        ClubTable table = clubTablesDao.findById(booking.getTableId()).orElseThrow(NotFoundException::new);
        return clubDao.findById(table.getClubId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClubLimitsDto> getLimits(UUID clubId) {
        Optional<Club> clubOpt = clubDao.findById(clubId);
        if (clubOpt.isEmpty()) {
            return Optional.empty();
        }

        Club club = clubOpt.get();

        List<ClubTable> tables = clubTablesDao.findByClub(club);

        int maxTables = tables.size();
        int maxCapacity = tables.stream()
                .mapToInt(ClubTable::getCapacity)
                .sum();

        return Optional.of(new ClubLimitsDto(
                clubId.toString(),
                maxCapacity,
                maxTables,
                1,
                10
        ));
    }


}
