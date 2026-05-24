package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Photo;

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
     Optional<Photo> findByBooking(UUID userId);

    Optional<String> getCoverUrl(UUID clubId);
    Map<UUID,String> getCoverUrls(Set<UUID> clubIds);
    void uploadPhotoForClub(UUID clubId, String userEmail, Photo file) throws IOException;

    void uploadCover(UUID clubId, UUID uploaderId, String filename, String contentType, byte[] data);
    UUID uploadCarousel(UUID clubId, UUID uploaderId, String filename, String contentType, byte[] data);
    List<Photo> getCarouselPhotos(UUID clubId);
    List<Photo> getClubPhotos(UUID clubId, int limit, int offset);
    Optional<Photo> getEventPhoto(UUID eventId);
    UUID uploadReviewPhoto(UUID clubId, UUID reviewId, UUID userId, Photo file) throws IOException;
    UUID uploadEventPhoto(UUID clubId, UUID eventId, UUID userId, Photo file) throws IOException;
    List<Photo> getReviewPhotosByClub(UUID clubId, int limit, int offset);
    Photo createPhoto(String filename, String contentType, byte[] data);
    Optional<Photo> getCoverPhoto(UUID clubId);
}
