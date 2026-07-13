package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.*;

@Repository
public class ReviewsJpaDao implements ReviewsDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Review create(User user, Club club, int rating, String description) {
        Review r = new Review();
        r.setUser(user);
        r.setClub(club);
        r.setRating(rating);
        r.setDescription(description);
        em.persist(r);
        return r;
    }

    @Override
    public Review update(UUID reviewId, int rating, String description) {
        Objects.requireNonNull(reviewId, "reviewId is required");
        Review r = em.find(Review.class, reviewId);
        if (r == null) return null;
        r.setRating(rating);
        r.setDescription(description);
        return r;
    }


    public Review upsert(User user, Club club, int rating, String description, boolean fromTableGuest) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(user.getId());
        Objects.requireNonNull(club);
        Objects.requireNonNull(club.getClubId());

        UUID provisionalId = UUID.randomUUID();

        em.createNativeQuery("""
        INSERT INTO reviews (review_id, user_id, club_id, rating, description, from_table_guest)
        VALUES (:rid, :uid, :cid, :rating, :desc, :ftg)
        ON CONFLICT (user_id, club_id) DO UPDATE
          SET rating = EXCLUDED.rating,
              description = EXCLUDED.description,
              from_table_guest = EXCLUDED.from_table_guest,
              created_at = NOW()
    """)
                .setParameter("rid", provisionalId)
                .setParameter("uid", user.getId())
                .setParameter("cid", club.getClubId())
                .setParameter("rating", rating)
                .setParameter("desc", description)
                .setParameter("ftg", fromTableGuest)
                .executeUpdate();

        return findByUserAndClub(user, club).orElse(null);
}
    @Override
    public boolean delete(UUID id) {
        Review r = em.find(Review.class, id);
        if (r == null) return false;
        em.remove(r);
        return true;
    }

    @Override
    public Optional<Review> findById(UUID id) {
        List<Review> results = em.createQuery("""
            SELECT r
            FROM Review r
            JOIN FETCH r.user
            JOIN FETCH r.club
            WHERE r.reviewId = :id
            """, Review.class)
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<Review> findByClub(Club club) {
        return em.createQuery("""
            SELECT r
            FROM Review r
            JOIN FETCH r.user
            WHERE r.club = :c
            ORDER BY r.createdAt DESC
            """, Review.class)
                .setParameter("c", club)
                .getResultList();
    }

    @Override
    public Optional<Review> findByUserAndClub(User user, Club club) {
        List<Review> results = em.createQuery("""
            SELECT r
            FROM Review r
            WHERE r.user = :user AND r.club = :club
            """, Review.class)
                .setParameter("user", user)
                .setParameter("club", club)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<Review> searchPaginated(UUID userId, UUID clubId, String sort, int limit, int offset) {

        String orderBy = switch (sort) {
            case "created_desc" -> "ORDER BY r.created_at DESC";
            case "created_asc"  -> "ORDER BY r.created_at ASC";
            case "rating_desc"  -> "ORDER BY r.rating DESC";
            case "rating_asc"   -> "ORDER BY r.rating ASC";
            default -> "ORDER BY r.created_at DESC";
        };


        StringBuilder sql = new StringBuilder("""
                    SELECT CAST(r.review_id AS TEXT) AS id_text
                    FROM reviews r
                    WHERE 1=1
                """);

        if (userId != null) sql.append(" AND r.user_id = :uid");
        if (clubId != null) sql.append(" AND r.club_id = :cid");

        sql.append(" ").append(orderBy);
        sql.append(" LIMIT :limit OFFSET :offset");

        var nativeQuery = em.createNativeQuery(sql.toString())
                .unwrap(NativeQuery.class)
                .addScalar("id_text", StandardBasicTypes.STRING)
                .setParameter("limit", limit)
                .setParameter("offset", offset);

        if (userId != null) {
            nativeQuery.setParameter("uid", userId);
        }
        if (clubId != null) {
            nativeQuery.setParameter("cid", clubId);
        }

        @SuppressWarnings("unchecked")
        List<String> idTexts = nativeQuery.getResultList();

        if (idTexts.isEmpty()) return List.of();

        List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();


        List<Review> batch = em.createQuery("""
                            SELECT r
                            FROM Review r
                            JOIN FETCH r.user u
                            JOIN FETCH r.club c
                            WHERE r.reviewId IN :ids
                        """, Review.class)
                .setParameter("ids", ids)
                .getResultList();



        Map<UUID, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            orderMap.put(ids.get(i), i);
        }

        batch.sort(Comparator.comparingInt(r -> orderMap.get(r.getReviewId())));

        return batch;
    }

    @Override
    public int countSearch(UUID userId, UUID clubId) {
        StringBuilder jpql = new StringBuilder("""
        SELECT COUNT(r)
        FROM Review r
        WHERE 1=1
    """);
        if (userId != null) jpql.append(" AND r.user.id = :uid");
        if (clubId != null) jpql.append(" AND r.club.clubId = :cid");

        TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class);
        if (userId != null) q.setParameter("uid", userId);
        if (clubId != null) q.setParameter("cid", clubId);

        Long total = q.getSingleResult();
        return total == null ? 0 : total.intValue();
    }

    @Override
    public Double averageByClubId(UUID clubId) {
        return em.createQuery("""
      SELECT AVG(r.rating)
      FROM Review r
      WHERE r.club.clubId = :cid
      """, Double.class)
                .setParameter("cid", clubId)
                .getSingleResult();
    }

    @Override
    public void updatePhotoId(UUID reviewId, UUID photoId) {
        Review review = em.find(Review.class, reviewId);
        if (review != null) {
            review.setPhotoId(photoId);
        }
    }

    @Override
    public void flush() {
        em.flush();
    }

    @Override
    public boolean existsByUserAndClub(UUID userId, UUID clubId) {
        Long count = em.createQuery(
                        "SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId AND r.club.clubId = :clubId", Long.class
                )
                .setParameter("userId", userId)
                .setParameter("clubId", clubId)
                .getSingleResult();
        return count > 0;
    }

    private String resolveOrderBy(String sort) {
        if (sort == null) return "ORDER BY r.createdAt DESC";
        return switch (sort) {
            case "created_asc" -> "ORDER BY r.createdAt ASC";
            case "rating_desc" -> "ORDER BY r.rating DESC, r.createdAt DESC";
            case "rating_asc" -> "ORDER BY r.rating ASC, r.createdAt DESC";
            default -> "ORDER BY r.createdAt DESC";
        };
    }

    @Override
    public List<Review> findByClubPaginated(UUID clubId, int page, int size) {
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        final String idSql = """
        SELECT CAST(r.review_id AS TEXT) AS id_text
        FROM reviews r
        WHERE r.club_id = :clubId
        ORDER BY r.created_at DESC, r.review_id DESC
        LIMIT :limit OFFSET :offset
    """;

        List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit", limit)
                .setParameter("offset", offset)
                .getResultList();

        if (idTexts.isEmpty()) return List.of();
        List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        return em.createQuery("""
        SELECT r FROM Review r
        JOIN FETCH r.user
        WHERE r.reviewId IN :ids
        ORDER BY r.createdAt DESC, r.reviewId DESC
    """, Review.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public int countByClubPaginated(UUID clubId) {
        return em.createQuery("""
        SELECT COUNT(r)
        FROM Review r
        WHERE r.club.clubId = :clubId
    """, Long.class)
                .setParameter("clubId", clubId)
                .getSingleResult()
                .intValue();
    }
}
