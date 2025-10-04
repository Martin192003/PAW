package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();

    Optional<Club> getClubById(UUID clubId);

    List<Club> advancedSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort);

    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar);

    Club updateClub(Club club);

    List<Club> getClubsByUser(UUID userId);

    List<User> getAdminsByClub(UUID clubId);

    List<Club> searchPaginated(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort, int limit, int offset);

    int countSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar);

    List<Club> getAllClubsPaginated(int limit, int offset);

    int countAllClubs();

    List<Club> getClubsByUserPaginated(UUID userId, int limit, int offset);

    int countClubsByUser(UUID userId);

    Map<UUID, String> mapClubNames(List<Booking> bookings);

    void assertAdmin(UUID clubId, UUID userId);

    void updateField(UUID clubId, String field, String value, UUID editorId);

    boolean sendContactMessage(UUID clubId, String senderEmail, String senderName, String senderPhone, String message);


}