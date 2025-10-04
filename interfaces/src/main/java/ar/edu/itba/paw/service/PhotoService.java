package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Photo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoService {

    UUID upload(UUID id,
                UUID postedBy,
                String filename,
                String contentType,
                byte[] data);

    List<Photo> listByClub(UUID clubId, int page, int pageSize);

    Optional<Photo> findById(UUID photoId);

    Optional<Photo> getWithId(UUID photoId);

    UUID save(Photo photo);

    boolean delete(UUID photoId);
    Optional<Photo> findByBooking(UUID userId);
    UUID uploadPhotoForClub(UUID clubId, String userEmail, MultipartFile file) throws IOException;

}