package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public Optional<Photo> findByBooking(UUID bookingId);

    List<Photo> findByClubIdPaginated(UUID clubId, int limit, int offset);

    Optional<Photo> findCoverByClubId(UUID clubId);
    List<Photo> findCarouselByClubId(UUID clubId, int limit, int offset);
    void deleteCoverByClubId(UUID clubId);

    UUID createReviewPhoto(UUID clubId, UUID reviewId, UUID postedBy,
                           String filename, String contentType, byte[] data);
    List<Photo> findReviewPhotosByClub(UUID clubId, int limit, int offset);
    Optional<Photo> findByReviewId(UUID reviewId);
    UUID createEventPhoto(UUID clubId, UUID eventId, UUID postedBy, String filename, String contentType, byte[] data);




}