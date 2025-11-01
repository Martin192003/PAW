package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Photo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

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
    public Optional<Photo> findByBooking(UUID userId);

    Optional<String> getCoverUrl(UUID clubId);
    Map<UUID,String> getCoverUrls(Set<UUID> clubIds);
    UUID uploadPhotoForClub(UUID clubId, String userEmail, MultipartFile file) throws IOException;

    void uploadCover(UUID clubId, UUID uploaderId, String filename, String contentType, byte[] data);
    void uploadCarousel(UUID clubId, UUID uploaderId, String filename, String contentType, byte[] data);
    Optional<Photo> getCoverPhoto(UUID clubId);
    List<Photo> getCarouselPhotos(UUID clubId, int limit, int offset);
    List<Photo> getClubPhotos(UUID clubId, int limit, int offset);

    UUID uploadReviewPhoto(UUID clubId, UUID reviewId, UUID userId, MultipartFile file) throws IOException;
    UUID uploadEventPhoto(UUID clubId, UUID eventId, UUID userId, MultipartFile file) throws IOException;
    List<Photo> getReviewPhotosByClub(UUID clubId, int limit, int offset);
    Optional<Photo> getPhotoByReview(UUID reviewId);
}