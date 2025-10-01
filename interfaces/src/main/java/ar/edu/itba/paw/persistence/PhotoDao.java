package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoDao {

    UUID create(UUID clubId,
                UUID postedBy,
                String filename,
                String contentType,
                byte[] data);

    List<Photo> findByClub(UUID clubId, int limit, int offset);

    Optional<Photo> findById(UUID photoId);

    Optional<Photo> findByIdWithData(UUID photoId);

    UUID save(Photo photo);

    boolean delete(UUID photoId);

    public Optional<Photo> findByBooking(UUID bookingId);

}