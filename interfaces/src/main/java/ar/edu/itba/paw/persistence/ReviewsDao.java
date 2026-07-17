package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReviewsDao {
    Optional<Review> findById(UUID id);
    List<Review> findByClub(Club club);

    List<Review> searchPaginated(UUID userId, UUID clubId, String sort, int limit, int offset);
    Map<UUID, UUID> searchLinkTargets(UUID userId, UUID clubId, String sort, int limit, int offset);
    int countSearch(UUID userId, UUID clubId);


    Double averageByClubId(UUID clubId);
    Review create(User user, Club club, int rating, String description);
    Review update(UUID reviewId, int rating, String description);
    boolean delete(UUID id);

    void updatePhotoId(UUID reviewId, UUID photoId);
    Review upsert(User user, Club club, int rating, String description, boolean fromTableGuest);
    Optional<Review> findByUserAndClub(User user, Club club);
    void flush();
    boolean existsByUserAndClub(UUID userId, UUID clubId);

    int countByClubPaginated(UUID clubId);
    List<Review> findByClubPaginated(UUID clubId, int page, int size);
}
