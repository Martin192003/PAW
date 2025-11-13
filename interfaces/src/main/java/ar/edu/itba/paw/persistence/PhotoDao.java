package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;

import java.util.*;

public interface PhotoDao {

    UUID create(UUID clubId,
                UUID postedBy,
                String filename,
                String contentType,
                byte[] data,
                PhotoType photoType);

    List<Photo> findByClub(UUID clubId, int limit, int offset);

    Optional<Photo> findById(UUID photoId);

    Optional<Photo> findByIdWithData(UUID photoId);

    UUID save(Photo photo);

    boolean delete(UUID photoId);

    Optional<Photo> findByBooking(UUID bookingId);

    Optional<Photo> findCoverByClubId(UUID clubId);
    List<Photo> findCarouselByClubId(UUID clubId);
    void deleteCoverByClubId(UUID clubId);

    UUID createReviewPhoto(UUID clubId, UUID reviewId, UUID postedBy,
                           String filename, String contentType, byte[] data);
    List<Photo> findReviewPhotosByClub(UUID clubId, int limit, int offset);
    UUID createEventPhoto(UUID clubId, UUID eventId, UUID postedBy, String filename, String contentType, byte[] data);
    List<Photo> findCarouselWithoutEventByClubId(UUID clubId, int limit, int offset);

    Map<UUID, String> findAllCoversByClubIds(Set<UUID> clubIds);

    long countByClubIdAndType(UUID clubId, PhotoType type);
}