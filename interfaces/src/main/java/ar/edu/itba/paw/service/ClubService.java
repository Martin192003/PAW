package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {

    Optional<Club> getClubById(UUID clubId);

    List<Club> advancedSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort);

    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar, MultipartFile image);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar);

    Club updateClub(Club club);

    List<Club> getClubsByUser(UUID userId);

    List<User> getAdminsByClub(UUID clubId);

    List<Club> searchPaginated(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort, int limit, int offset);

    int countSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar);

    List<Club> getClubsByUserPaginated(UUID userId, int limit, int offset);

    int countClubsByUser(UUID userId);

    Map<UUID, String> mapClubNames(List<Booking> bookings);

    void assertAdmin(UUID clubId, UUID userId);

    boolean sendContactMessage(UUID clubId, String senderEmail, String senderName, String senderPhone, String message);

    List<Club> getClubsWithBookingsByUser(UUID userId);

    List<Club> searchAvailablePaginated(String nameLike, Zone location, String musicGenre,
                                        String priceRange, Boolean isBar, String sort,
                                        LocalDate day, int limit, int offset);

    int countSearchAvailable(String nameLike, Zone location, String musicGenre,
                             String priceRange, Boolean isBar, LocalDate day);
}