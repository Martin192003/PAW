package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidClubException;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.EventDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventDao eventDao;
    private final PhotoService photoService;
    private final SecurityService securityService;

    @Autowired
    public EventServiceImpl(EventDao eventDao, PhotoService photoService, SecurityService securityService) {
        this.eventDao = eventDao;
        this.photoService = photoService;
        this.securityService = securityService;
    }

    @Override
    @Transactional
    public Event create(UUID clubId, String eventName, String description, LocalDate date) {
        validateName(eventName);
        validateDate(date);

        return eventDao.create(clubId, eventName.trim(), trimOrNull(description), date);
    }


    @Override
    @Transactional
    public Event update(UUID clubId, UUID eventId, String eventName, String description, LocalDate date, Photo image) {
        validateName(eventName);
        validateDate(date);

        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");

        UUID photoId = current.getPhotoId();

        if (image != null && image.getData() != null && image.getData().length > 0) {
            try {
                User loggedUser = securityService.getVerifiedLoggedUser();
                photoId = photoService.uploadEventPhoto(
                        clubId,
                        current.getEventId(),
                        loggedUser.getId(),
                        image
                );
            } catch (IOException e) {
                throw new RuntimeException("Error collecting event image", e);
            }
        }

        final Event updated = eventDao.update(eventId, clubId, eventName.trim(), trimOrNull(description), date, photoId);
        if (updated == null)
            throw new NotFoundException("Event not found");

        return updated;
    }


    @Override
    @Transactional
    public void delete(UUID clubId, UUID eventId) {
        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");
        if (!eventDao.delete(eventId))
            throw new NotFoundException("Event not found");
        if (current.getPhotoId() != null) {
            photoService.delete(current.getPhotoId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Event get(UUID eventId) {
        return eventDao.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> listByClub(UUID clubId, int page, int size) {
        validatePaging(page, size);
        return eventDao.findByClub(clubId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByClub(UUID clubId) {
        return eventDao.countByClub(clubId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<Event> listUpcoming(int page, int size) {
        validatePaging(page, size);
        return eventDao.findUpcoming(LocalDate.now(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUpcoming() {
        return eventDao.countUpcoming(LocalDate.now());
    }

    // ---------- Helpers ----------

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new ValidationException("eventName is required");
        if (name.trim().length() > 120)
            throw new ValidationException("eventName too long (max 120)");
    }

    private void validateDate(LocalDate date) {
        if (date == null) throw new InvalidDateRangeException("date is required");
        if (date.isBefore(LocalDate.now()))
            throw new InvalidDateRangeException("date cannot be in the past");
    }

    private void validatePaging(int page, int size) {
        if (page < 0) throw new ValidationException("page must be >= 0");
        if (size <= 0 || size > 100) throw new ValidationException("size must be in 1..100");
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        final String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findUpcomingByClub(UUID clubId, int offset, int limit) {
        return eventDao.findUpcomingByClub(clubId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findPastByClub(UUID clubId, int offset, int limit) {
        return eventDao.findPastByClub(clubId, offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public int countUpcomingByClub(UUID clubId) {
        return eventDao.countUpcomingByClub(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPastByClub(UUID clubId) {
        return eventDao.countPastByClub(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public int totalEventPagesPast(UUID clubId, int size) {
        if (size <= 0) return 0;
        int totalPastEvents = countPastByClub(clubId);

        return (int) Math.ceil((double) totalPastEvents / size);

    }

    @Override
    @Transactional(readOnly = true)
    public int totalEventPagesUpcoming(UUID clubId, int size) {
        if (size <= 0) return 0;
        int totalUpcomigEvents = countUpcomingByClub(clubId);

        return (int) Math.ceil((double) totalUpcomigEvents / size);
    }

    @Override
    @Transactional(readOnly = true)
    public int totalEventPages(UUID clubId, int size) {
        if (size <= 0) return 0;
        int totalEvents = countByClub(clubId);

        return (int) Math.ceil((double) totalEvents / size);
    }

    @Override
    @Transactional
    public UUID setEventPhoto(UUID clubId, UUID eventId, Photo image) {
        if (image == null || image.getData() == null || image.getData().length == 0) {
            throw new ValidationException("image data is required");
        }
        if (image.getFilename() == null || image.getFilename().trim().isEmpty()) {
            throw new ValidationException("filename is required");
        }
        if (image.getContentType() == null || image.getContentType().trim().isEmpty()) {
            throw new ValidationException("contentType is required");
        }

        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");

        UUID oldPhotoId = current.getPhotoId();

        try {
           User loggedUser = securityService.getVerifiedLoggedUser();

            UUID newPhotoId = photoService.uploadEventPhoto(
                    clubId,
                    eventId,
                    loggedUser.getId(),
                    image
            );

            eventDao.attachPhoto(eventId, newPhotoId);

            if (oldPhotoId != null && !oldPhotoId.equals(newPhotoId)) {
                photoService.delete(oldPhotoId);
            }

            return newPhotoId;
        } catch (IOException e) {
            throw new RuntimeException("Error uploading event image", e);
        }
    }

    @Override
    @Transactional
    public void deleteEventPhoto(UUID clubId, UUID eventId) {
        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");

        UUID oldPhotoId = current.getPhotoId();
        if (oldPhotoId == null) return;

        eventDao.attachPhoto(eventId, null);
        current.setPhotoId(null);
        photoService.delete(oldPhotoId);
    }

    @Override
    @Transactional
    public Optional<UUID> getEventPhotoId(UUID clubId, UUID eventId){
        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));

        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");

        return Optional.ofNullable(current.getPhotoId());
    }
    @Override
    @Transactional
    public Photo getEventPhoto(UUID clubId, UUID eventId) {
        getEventPhotoId(clubId, eventId)
                .orElseThrow(() -> new NotFoundException("Event photo not found"));

        return photoService.getEventPhoto(eventId)
                .orElseThrow(() -> new NotFoundException("Event photo not found"));
    }

}
