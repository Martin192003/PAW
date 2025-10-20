package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Event;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class EventJpaDao implements EventDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Event create(UUID clubId, String eventName, String description, LocalDate date, UUID photoId) {
        final Event event = new Event(UUID.randomUUID(), clubId, eventName, description, date, photoId);
        em.persist(event);
        return event;
    }

    @Override
    public Event update(UUID eventId, UUID clubId, String eventName, String description, LocalDate date, UUID photoId) {
        final Event event = em.find(Event.class, eventId);
        if (event == null) {
            return null;
        }
        event.setClubId(clubId);
        event.setEventName(eventName);
        event.setDescription(description);
        event.setDate(date);
        event.setPhotoId(photoId);
        return event;
    }

    @Override
    public boolean delete(UUID eventId) {
        final Event event = em.find(Event.class, eventId);
        if (event == null) return false;
        em.remove(event);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Event> findById(UUID eventId) {
        return Optional.ofNullable(em.find(Event.class, eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> findByClub(UUID clubId, int page, int size) {
        return em.createQuery(
                        "FROM Event e WHERE e.clubId = :clubId ORDER BY e.date DESC", Event.class)
                .setParameter("clubId", clubId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Event> searchByDateRangePaginated(LocalDate startDate, LocalDate endDate, int page, int size) {
        return em.createQuery(
                        "FROM Event e WHERE e.date BETWEEN :start AND :end ORDER BY e.date ASC", Event.class)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countByDateRange(LocalDate startDate, LocalDate endDate) {
        final Long total = em.createQuery(
                        "SELECT COUNT(e) FROM Event e WHERE e.date BETWEEN :start AND :end", Long.class)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getSingleResult();
        return total.intValue();
    }

    @Override
    @Transactional(readOnly = true)
    public int countByClub(UUID clubId) {
        final Long total = em.createQuery(
                        "SELECT COUNT(e) FROM Event e WHERE e.clubId = :clubId", Long.class)
                .setParameter("clubId", clubId)
                .getSingleResult();
        return total.intValue();
    }
}
