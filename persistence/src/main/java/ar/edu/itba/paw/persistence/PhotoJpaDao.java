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
    public List<Photo> findByClub(UUID clubId, int limit, int offset) {
        return em.createQuery("""
                SELECT p
                FROM Photo p
                WHERE p.clubId = :club
                ORDER BY p.createdAt DESC
                """, Photo.class)
                .setParameter("club", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> findByIdWithData(UUID photoId) {
        return Optional.ofNullable(em.find(Photo.class, photoId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> findById(UUID photoId) {
        return Optional.ofNullable(em.find(Photo.class, photoId));
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
