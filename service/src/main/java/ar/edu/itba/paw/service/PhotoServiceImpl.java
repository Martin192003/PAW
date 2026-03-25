
package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.PhotoType;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.persistence.EventDao;
import ar.edu.itba.paw.persistence.PhotoDao;
import ar.edu.itba.paw.model.Photo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.*;

@Service
public class PhotoServiceImpl implements PhotoService {


    private final PhotoDao photoDao;
    private final UserService userService;
    private final EventDao eventDao;

    public PhotoServiceImpl(PhotoDao photoDao, UserService userService, EventDao eventDao) {
        this.photoDao = photoDao;
        this.userService = userService;
        this.eventDao = eventDao;
    }

    @Override
    @Transactional
    public UUID upload(UUID clubId, UUID userId, String filename, String contentType, byte[] data) {
        Photo photo = new Photo();
        photo.setClubId(clubId);
        photo.setPostedBy(userId);
        photo.setFilename(filename);
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setPhotoType(PhotoType.CAROUSEL);

        return photoDao.save(photo);
    }

    @Override
    @Transactional
    public void uploadCover(UUID clubId, UUID userId, String filename, String contentType, byte[] data) {
        photoDao.deleteCoverByClubId(clubId);

        Photo photo = new Photo();
        photo.setClubId(clubId);
        photo.setPostedBy(userId);
        photo.setFilename(filename);
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setPhotoType(PhotoType.COVER);

        photoDao.save(photo);
    }

    @Override
    @Transactional
    public void uploadCarousel(UUID clubId, UUID userId, String filename, String contentType, byte[] data) {
        long existingPhotos = photoDao.countByClubIdAndType(clubId, PhotoType.CAROUSEL);

        if (existingPhotos >= 20) {
            throw new IllegalStateException("No se pueden subir más de 20 fotos al club.");
        }
        Photo photo = new Photo();
        photo.setClubId(clubId);
        photo.setPostedBy(userId);
        photo.setFilename(filename);
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setPhotoType(PhotoType.CAROUSEL);

        photoDao.save(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> listByClub(UUID clubId, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, pageSize);
        int offset = safePage * safeSize;
        return photoDao.findByClub(clubId, safeSize, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> findByBooking(UUID bookingId){
        return photoDao.findByBooking(bookingId);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> findById(UUID photoId) {
        return photoDao.findById(photoId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Photo> getWithId(UUID photoId) {
        return photoDao.findByIdWithData(photoId);
    }

    @Override
    @Transactional
    public UUID save(Photo p) {
        return photoDao.save(p);
    }

    @Override
    @Transactional
    public boolean delete(UUID photoId) {
        return photoDao.delete(photoId);
    }

    @Override
    @Transactional
    public void uploadPhotoForClub(UUID clubId, String userEmail, Photo file){
        if (file == null) {
            throw new IllegalArgumentException("File is empty");
        }

        User effectiveUser = (userEmail != null)
                ? userService.findByEmail(userEmail)
                : userService.findByEmail("dev@local");

        if (effectiveUser == null) {
            throw new IllegalArgumentException("User not found");
        }

        upload(
                clubId,
                effectiveUser.getId(),
                file.getFilename(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getData()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getCoverUrl(UUID clubId) {
        Optional<Photo> coverOpt = photoDao.findCoverByClubId(clubId);
        if (coverOpt.isPresent()) {
            Photo cover = coverOpt.get();
            if (cover.getPhotoId() != null && eventDao.findByPhotoIdAndClub(cover.getPhotoId(), clubId).isEmpty()) {
                return Optional.ofNullable(cover.getUrl());
            }
        }

        List<Photo> carousel = photoDao.findCarouselWithoutEventByClubId(clubId, 50, 0);
        return carousel.stream().min((p1, p2) -> {
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null)
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    return p2.getPhotoId().compareTo(p1.getPhotoId());
                })
                .map(Photo::getUrl);
    }


    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> getCoverUrls(Set<UUID> clubIds) {
        return photoDao.findAllCoversByClubIds(clubIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getCarouselPhotos(UUID clubId) {
        List<Photo> photos = photoDao.findCarouselByClubId(clubId);
        System.out.println("getCarouselPhotos for clubId " + clubId + ": " +
                photos.stream().map(p -> p.getUrl() + " [type: " + p.getPhotoType() + "]").toList());
        return photos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getClubPhotos(UUID clubId, int limit, int offset) {
        List<Photo> result = new ArrayList<>();
        photoDao.findCoverByClubId(clubId).ifPresent(result::add);
        result.addAll(photoDao.findCarouselByClubId(clubId));
        return result;
    }

    @Override
    @Transactional
    public UUID uploadReviewPhoto(UUID clubId, UUID reviewId, UUID userId, Photo file) {
        if (file == null ) throw new IllegalArgumentException("File is empty");
        final String ct = (file.getContentType() != null) ? file.getContentType() : "application/octet-stream";
        return photoDao.createReviewPhoto(
                clubId, reviewId, userId,
                file.getFilename(), ct, file.getData()
        );
    }

    @Override
    @Transactional
    public Optional<Photo> getEventPhoto(UUID eventId) {
        Event event = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        return photoDao.findByIdWithData(event.getPhotoId());
    }

    @Override
    @Transactional
    public UUID uploadEventPhoto(UUID clubId, UUID eventId, UUID userId, Photo file) {
        Objects.requireNonNull(clubId,  "clubId no puede ser null");
        Objects.requireNonNull(eventId, "eventId no puede ser null");
        Objects.requireNonNull(userId,  "userId no puede ser null");

        if (file == null) {
            throw new IllegalArgumentException("El archivo de la foto del evento está vacío o es null");
        }

        final String filename    = file.getFilename();
        final String contentType = (file.getContentType() != null) ? file.getContentType() : "application/octet-stream";
        final byte[]  data       = file.getData();

        return photoDao.createEventPhoto(clubId, eventId, userId, filename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> getReviewPhotosByClub(UUID clubId, int limit, int offset) {
        return photoDao.findReviewPhotosByClub(clubId, limit, offset);
    }

    @Override
    public Photo createPhoto(String filename, String contentType, byte[] data) {
        Photo photo = new Photo();
        photo.setFilename(filename);
        photo.setContentType(contentType);
        photo.setData(data);
        return photo;
    }

}