package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class PhotoJpaDao implements PhotoDao {

    @PersistenceContext
    private EntityManager em;

    private final int MAX_LIMIT_CAROUSEL = 20;
    private final int OFFSET_CAROUSEL = 0;

    @Override
    public UUID create(UUID clubId, UUID postedBy, String filename, String contentType, byte[] data, PhotoType photoType) {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(postedBy);
        p.setFilename(filename);
        p.setContentType(contentType);
        p.setData(data);
        p.setPhotoType(photoType);
        em.persist(p);
        return p.getPhotoId();
    }

    @Override
    public List<Photo> findByClub(final UUID clubId, final int limit, final int offset) {
        final int lim = Math.max(0, limit);
        final int off = Math.max(0, offset);

        // 1) Página de IDs
        @SuppressWarnings("unchecked")
        final List<String> idTexts = em.createNativeQuery("""
                        SELECT CAST(p.photo_id AS TEXT) AS id_text
                        FROM photos p
                        WHERE p.club_id = :clubId
                        ORDER BY p.created_at DESC NULLS LAST, p.photo_id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit", lim)
                .setParameter("offset", off)
                .getResultList();

        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        final List<Photo> photos = em.createQuery("""
            SELECT p
            FROM Photo p
            WHERE p.photoId IN :ids
            ORDER BY p.createdAt DESC, p.photoId DESC
        """, Photo.class)
                .setParameter("ids", ids)
                .getResultList();

        photos.forEach(p -> { em.detach(p); p.setData(null); });
        return photos;
    }


    @Override
    public Optional<Photo> findByIdWithData(UUID photoId) {
        return Optional.ofNullable(em.find(Photo.class, photoId));
    }

    @Override
    public Optional<Photo> findById(UUID id) {
        Photo p = em.find(Photo.class, id);
        if (p == null) return Optional.empty();
        em.detach(p);
        p.setData(null);
        return Optional.of(p);
    }
    @Override

    public UUID save(Photo p) {
        if (p.getPhotoId() == null) {
            em.persist(p);
            return p.getPhotoId();
        } else {
            Photo merged = em.merge(p);
            return merged.getPhotoId();
        }
    }

    @Override
    public Optional<Photo> findByBooking(UUID bookingId) {
        List<Photo> list = em.createNativeQuery("""
        SELECT p.*
        FROM photos p
        JOIN bookings b ON p.posted_by = b.user_id
        WHERE b.booking_id = ?1
        """, Photo.class)
                .setParameter(1, bookingId)
                .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.getFirst());
    }

    @Override
    public boolean delete(UUID photoId) {
        Photo p = em.find(Photo.class, photoId);
        if (p == null) return false;
        em.remove(p);
        return true;
    }

    @Override
    public Optional<Photo> findCoverByClubId(UUID clubId) {
        return em.createQuery(
                        "SELECT p FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type", Photo.class)
                .setParameter("clubId", clubId)
                .setParameter("type", PhotoType.COVER)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<Photo> findCarouselByClubId(UUID clubId) {
        return em.createQuery(
                        "SELECT p FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type ORDER BY p.createdAt DESC", Photo.class)
                .setParameter("clubId", clubId)
                .setParameter("type", PhotoType.CAROUSEL)
                .setFirstResult(OFFSET_CAROUSEL)
                .setMaxResults(MAX_LIMIT_CAROUSEL)
                .getResultList();
    }

    @Override
    public void deleteCoverByClubId(UUID clubId) {
        em.createQuery("DELETE FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type")
                .setParameter("clubId", clubId)
                .setParameter("type", PhotoType.COVER)
                .executeUpdate();
    }

    @Override
    public UUID createReviewPhoto(UUID clubId, UUID reviewId, UUID postedBy,
                                  String filename, String contentType, byte[] data) {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setReviewId(reviewId);
        p.setPostedBy(postedBy);
        p.setFilename(filename);
        p.setContentType(contentType);
        p.setData(data);
        p.setPhotoType(PhotoType.REVIEW);
        em.persist(p);
        return p.getPhotoId();
    }

    @Override
    public UUID createEventPhoto(UUID clubId, UUID eventId, UUID postedBy,
                                 String filename, String contentType, byte[] data) {
        Photo p = new Photo();
        p.setClubId(clubId);

        p.setReviewId(null);

        p.setPostedBy(postedBy);
        p.setFilename(filename);
        p.setContentType(contentType);
        p.setData(data);
        p.setPhotoType(PhotoType.EVENT);
        em.persist(p);
        return p.getPhotoId();
    }


    @Override
    public List<Photo> findReviewPhotosByClub(UUID clubId, int limit, int offset) {
        final int lim = Math.max(0, limit);
        final int off = Math.max(0, offset);

        // 1) Página de IDs
        @SuppressWarnings("unchecked") final List<String> idTexts = em.createNativeQuery("""
                SELECT CAST(p.photo_id AS TEXT) AS id_text
                FROM photos p
                WHERE p.club_id = :clubId AND p.photo_type = 'REVIEW'
                ORDER BY p.created_at DESC NULLS LAST, p.photo_id DESC
                LIMIT :limit OFFSET :offset
            """)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit", lim)
                .setParameter("offset", off)
                .getResultList();

        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades
        final List<Photo> photos = em.createQuery("""
                        SELECT p
                        FROM Photo p
                        WHERE p.photoId IN :ids AND p.photoType = :type
                        ORDER BY p.createdAt DESC, p.photoId DESC
                    """, Photo.class)
            .setParameter("ids", ids)
            .setParameter("type", PhotoType.REVIEW)
            .getResultList();

        photos.forEach(p -> {
            em.detach(p);
            p.setData(null);
        });
        return photos;
    }


    @Override
    public List<Photo> findCarouselWithoutEventByClubId(UUID clubId, int limit, int offset) {
        final List<String> idTexts = em.createNativeQuery("""
            SELECT CAST(p.photo_id AS TEXT) AS id_text
            FROM photos p
            LEFT JOIN event e ON e.photoid = p.photo_id
            WHERE p.club_id = :clubId
              AND p.photo_type = 'CAROUSEL'
              AND e.photoid IS NULL
            ORDER BY p.created_at DESC NULLS LAST, p.photo_id DESC
            LIMIT :limit OFFSET :offset
        """)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit", Math.max(0, limit))
                .setParameter("offset", Math.max(0, offset))
                .getResultList();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        if (ids.isEmpty()) return List.of();
        List<Photo> photos = em.createQuery("""
                            SELECT p
                            FROM Photo p
                            WHERE p.photoId IN :ids
                            ORDER BY p.createdAt DESC, p.photoId DESC
                        """, Photo.class)
                .setParameter("ids", ids)
                .getResultList();
        photos.forEach(p -> {
            em.detach(p);
            p.setData(null);
        });
        return photos;
    }



    @Override
    public Map<UUID, String> findAllCoversByClubIds(Set<UUID> clubIds) {
        List<Object[]> rows = em.createQuery("""
        SELECT c.clubId, p.url
        FROM Photo p
        JOIN Club c ON p.clubId = c.clubId
        WHERE p.photoType = :type
          AND p.clubId IN :clubIds
    """, Object[].class)
                .setParameter("type", PhotoType.COVER)
                .setParameter("clubIds", clubIds)
                .getResultList();

        Map<UUID, String> covers = rows.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (String) row[1],
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return covers;
    }

    public long countByClubIdAndType(UUID clubId, PhotoType type) {
        return em.createQuery(
                        "SELECT COUNT(p) FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type", Long.class)
                .setParameter("clubId", clubId)
                .setParameter("type", type)
                .getSingleResult();
    }
}
