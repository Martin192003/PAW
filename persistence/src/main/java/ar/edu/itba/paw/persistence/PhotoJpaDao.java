package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
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

    public List<Photo> findByClub(UUID clubId, int limit, int offset) {
        List<Photo> list = em.createQuery(
                        "select p from Photo p where p.clubId = :clubId order by p.createdAt desc", Photo.class)
                .setParameter("clubId", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
        list.forEach(p -> { em.detach(p); p.setData(null); });
        return list;
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

    public List<Photo> findByClubIdPaginated(UUID clubId, int limit, int offset) {
        return em.createQuery("SELECT p FROM Photo p WHERE p.clubId = :clubId ORDER BY p.createdAt DESC", Photo.class)
                .setParameter("clubId", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<Photo> findCarouselByClubId(UUID clubId, int limit, int offset) {
        return em.createQuery(
                        "SELECT p FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type ORDER BY p.createdAt DESC", Photo.class)
                .setParameter("clubId", clubId)
                .setParameter("type", PhotoType.CAROUSEL)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional
    public void deleteCoverByClubId(UUID clubId) {
        em.createQuery("DELETE FROM Photo p WHERE p.clubId = :clubId AND p.photoType = :type")
                .setParameter("clubId", clubId)
                .setParameter("type", PhotoType.COVER)
                .executeUpdate();
    }
}
