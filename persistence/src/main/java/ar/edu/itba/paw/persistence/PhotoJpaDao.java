package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.*;

@Repository
public class PhotoJpaDao implements PhotoDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public UUID create(UUID clubId, UUID postedBy, String filename, String contentType, byte[] data) {
        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(postedBy);
        p.setFilename(filename);
        p.setContentType(contentType);
        p.setData(data);
        em.persist(p);
        return p.getPhotoId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> findByClub(final UUID clubId, final int limit, final int offset) {
        final int lim = Math.max(0, limit);
        final int off = Math.max(0, offset);

        // 1) Página de IDs (SQL nativo) — evita traer el BLOB `data`
        final String idSql = """
        SELECT CAST(p.photo_id AS TEXT) AS id_text
        FROM photos p
        WHERE p.club_id = :clubId
        ORDER BY p.created_at DESC NULLS LAST, p.photo_id DESC
        LIMIT :limit OFFSET :offset
    """;

        @SuppressWarnings("unchecked")
        final List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit", lim)
                .setParameter("offset", off)
                .getResultList();

        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades por IDs (JPQL) con el mismo orden lógico
        final List<Photo> photos = em.createQuery("""
            SELECT p
            FROM Photo p
            WHERE p.photoId IN :ids
            ORDER BY p.createdAt DESC, p.photoId DESC
        """, Photo.class)
                .setParameter("ids", ids)
                .getResultList();

        // No exponemos el BLOB en listados
        photos.forEach(p -> { em.detach(p); p.setData(null); });
        return photos;
    }


    @Override
    @Transactional(readOnly = true)
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
    @Transactional
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
}
