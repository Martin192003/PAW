package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.*;

@Repository
public class ReviewsJpaDao implements ReviewsDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Review create(User user, Club club, int rating, String description) {
        Review r = new Review();
        r.setUser(user);               // Deben ser entidades administradas (o con ID válido)
        r.setClub(club);
        r.setRating(rating);
        r.setDescription(description);
        em.persist(r);
        return r;
    }

    @Override
    @Transactional
    public Review update(UUID reviewId, int rating, String description) {
        Objects.requireNonNull(reviewId, "reviewId is required");
        Review r = em.find(Review.class, reviewId);
        if (r == null) return null;
        r.setRating(rating);
        r.setDescription(description);
        // created_at lo maneja la DB / se actualiza en upsert nativo si corresponde
        return r;
    }

    /** Emula tu INSERT ... ON CONFLICT de Postgres con SQL nativo y mapeo a entidad. */
    @Transactional
    public Review upsert(User user, Club club, int rating, String description) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(user.getId());
        Objects.requireNonNull(club);
        Objects.requireNonNull(club.getClubId());

        UUID provisionalId = UUID.randomUUID();

        // Pass UUID objects directly to setParameter for PostgreSQL
        em.createNativeQuery("""
    INSERT INTO reviews (review_id, user_id, club_id, rating, description)
    VALUES (:rid, :uid, :cid, :rating, :desc)
    ON CONFLICT (user_id, club_id) DO UPDATE
      SET rating = EXCLUDED.rating,
          description = EXCLUDED.description,
          created_at = NOW()
""")
                .setParameter("rid", provisionalId) // UUID, not String
                .setParameter("uid", user.getId())  // UUID, not String
                .setParameter("cid", club.getClubId()) // UUID, not String
                .setParameter("rating", rating)
                .setParameter("desc", description)
                .executeUpdate();

        return findByUserAndClub(user, club).orElse(null);
}
    @Override
    @Transactional
    public boolean delete(UUID id) {
        Review r = em.find(Review.class, id);
        if (r == null) return false;
        em.remove(r);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(UUID id) {
        return Optional.ofNullable(em.find(Review.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return em.createQuery("""
                SELECT r
                FROM Review r
                ORDER BY r.createdAt DESC
                """, Review.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findByUser(User user) {
        return em.createQuery("""
                SELECT r
                FROM Review r
                WHERE r.user = :u
                ORDER BY r.createdAt DESC
                """, Review.class)
                .setParameter("u", user)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Optional<Review> findByUserAndClub(User user, Club club) {
        List<Review> results = em.createQuery("""
            SELECT r
            FROM Review r
            WHERE r.user = :user AND r.club = :club
            """, Review.class)
                .setParameter("user", user)
                .setParameter("club", club)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
