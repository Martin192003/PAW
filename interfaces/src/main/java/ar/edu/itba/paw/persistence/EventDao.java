package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Event;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventDao {
    Event create(UUID clubId, String eventName, String description, LocalDate date, UUID photoId);

    Event update(UUID eventId, UUID clubId, String eventName, String description, LocalDate date, UUID photoId);

    boolean delete(UUID eventId);

    Optional<Event> findById(UUID eventId);

    List<Event> findByClub(UUID clubId, int page, int size);

    List<Event> searchByDateRangePaginated(LocalDate startDate, LocalDate endDate, int page, int size);

    int countByDateRange(LocalDate startDate, LocalDate endDate);

    int countByClub(UUID clubId);

    List<Event> findUpcoming(LocalDate fromInclusive, int page, int size);

    int countUpcoming(LocalDate fromInclusive);
}