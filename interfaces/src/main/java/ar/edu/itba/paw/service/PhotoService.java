package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Photo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoService {

    UUID upload(UUID clubId,
                UUID postedBy,
                String filename,
                String contentType,
                byte[] data);

    List<Photo> listByClub(UUID clubId, int page, int pageSize);

    Optional<Photo> findById(UUID photoId);

    Optional<Photo> getWithId(UUID photoId);

    Photo save(Photo photo);

    boolean delete(UUID photoId);
}