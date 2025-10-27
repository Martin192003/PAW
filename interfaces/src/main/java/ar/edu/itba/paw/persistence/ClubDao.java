package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubDao {
    Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu);

    Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar, String cbu);

    boolean delete(UUID clubId);

    Optional<Club> findById(UUID clubId);

    Integer incrementQuantityRatings(UUID clubId, int delta);

    List<Club> search(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort);

    List<Club> findByUserId(UUID userId);

    List<User> findAdminsByClubId(UUID clubId);

    List<Club> findAllPaginated(int limit, int offset);

    int countAll();

    List<Club> searchPaginated(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar, String sort, int limit, int offset);

    int countSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar);

    List<Club> findByUserIdPaginated(UUID userId, int limit, int offset);

    int countByUserId(UUID userId);

    List<UUID> findIdsByAdmin(UUID adminUserId);

    List<Club> findClubsWithBookingsByUserId(UUID userId);

    List<Club> searchAvailablePaginated(String nameLike, Zone location, String musicGenre,
                                        String priceRange, Boolean isBar, String sort,
                                        LocalDate day, int limit, int offset);

    int countSearchAvailable(String nameLike, Zone location, String musicGenre,
                             String priceRange, Boolean isBar, LocalDate day);

}