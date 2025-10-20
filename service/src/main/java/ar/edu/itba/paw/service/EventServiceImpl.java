package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.exceptions.InvalidClubException;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.EventDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class EventServiceImpl implements EventService {


    private final EventDao eventDao;

    public EventServiceImpl(EventDao eventDao) { this.eventDao = eventDao; }

    @Override
    @Transactional
    public Event create(UUID clubId, String eventName, String description, LocalDate date, UUID photoId) {
        validateName(eventName);
        validateDate(date);
        return eventDao.create(clubId, eventName.trim(), trimOrNull(description), date, photoId);
    }

    @Override
    @Transactional
    public Event update(UUID clubId, UUID eventId, String eventName, String description, LocalDate date, UUID photoId) {
        validateName(eventName);
        validateDate(date);
        final Event current = eventDao.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!Objects.equals(current.getClubId(), clubId))
            throw new InvalidClubException("Event does not belong to the provided club");
        final Event updated = eventDao.update(eventId, clubId, eventName.trim(), trimOrNull(description), date, photoId);
        if (updated == null) throw new NotFoundException("Event not found");
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
        return eventDao.findByClub(clubId, page, size); // Query #1 (data)
    }


    @Override
    @Transactional(readOnly = true)
    public int countByClub(UUID clubId) { return eventDao.countByClub(clubId); } // Query +1 (count)




    @Override
    @Transactional(readOnly = true)
    public List<Event> searchByDate(LocalDate start, LocalDate end, int page, int size) {
        validatePaging(page, size);
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) throw new InvalidDateRangeException("end date cannot be before start date");
        return eventDao.searchByDateRangePaginated(start, end, page, size); // Query #1 (data)
    }


    @Override
    @Transactional(readOnly = true)
    public int countByDate(LocalDate start, LocalDate end) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) throw new InvalidDateRangeException("end date cannot be before start date");
        return eventDao.countByDateRange(start, end); // Query +1 (count)
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

}