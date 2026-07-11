package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Event;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.*;

@Repository
public class EventJpaDao implements EventDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Event create(UUID clubId, String eventName, String description, LocalDate date) {
        final Event event = new Event(UUID.randomUUID(), clubId, eventName, description, date, null);
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
    public Optional<Event> findById(UUID eventId) {
        return Optional.ofNullable(em.find(Event.class, eventId));
    }

    @Override
    public List<Event> findByClub(final UUID clubId, final int page, final int size) {
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        if (limit == 0) {
            return List.of();
        }

        return em.createQuery("""
                    SELECT e FROM Event e
                    WHERE e.clubId = :clubId
                    ORDER BY e.date DESC, e.eventId DESC
                    """, Event.class)
                .setParameter("clubId", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public int countByClub(UUID clubId) {
        final Long total = em.createQuery(
                        "SELECT COUNT(e) FROM Event e WHERE e.clubId = :clubId", Long.class)
                .setParameter("clubId", clubId)
                .getSingleResult();
        return total.intValue();
    }

    @Override
    public List<Event> findUpcoming(final LocalDate fromInclusive,
                                    final int page,
                                    final int size) {

        if (fromInclusive == null) {
            return List.of();
        }

        final int limit = Math.max(0, size);
        if (limit == 0) {
            return List.of();
        }

        final int safePage = Math.max(0, page);
        final int offset   = safePage * limit;

        return em.createQuery("""
                    SELECT e FROM Event e
                    WHERE e.date >= :from
                    ORDER BY e.date ASC, e.eventId ASC
                    """, Event.class)
                .setParameter("from", fromInclusive)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }


    @Override
    public int countUpcoming(LocalDate fromInclusive) {
        final Long total = em.createQuery(
                        "SELECT COUNT(e) FROM Event e WHERE e.date >= :from", Long.class)
                .setParameter("from", fromInclusive)
                .getSingleResult();
        return total.intValue();
    }

    @Override
    public Optional<Event> findByPhotoIdAndClub(UUID photoId, UUID clubId) {
        final List<Event> events = em.createQuery(
                        "SELECT e FROM Event e WHERE e.photoId = :photoId AND e.clubId = :clubId", Event.class)
                .setParameter("photoId", photoId)
                .setParameter("clubId", clubId)
                .getResultList();
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(0));
    }

    @Override
    public void attachPhoto(UUID eventId, UUID photoId) {
        Event ev = em.find(Event.class, eventId);
        if (ev == null) throw new IllegalArgumentException("Evento no encontrado");
        ev.setPhotoId(photoId);
        em.merge(ev);
    }

    @Override
    public List<Event> findUpcomingByClub(UUID clubId, int page, int size) {

        if (clubId == null) {
            return List.of();
        }

        final int limit = Math.max(0, size);
        if (limit == 0) {
            return List.of();
        }

        final int safePage = Math.max(0, page);
        final int offset   = safePage * limit;

        LocalDate today = LocalDate.now();

        return em.createQuery("""
                    SELECT e FROM Event e
                    WHERE e.clubId = :clubId
                      AND e.date >= :today
                    ORDER BY e.date ASC, e.eventId ASC
                    """, Event.class)
                .setParameter("clubId", clubId)
                .setParameter("today", today)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public List<Event> findPastByClub(UUID clubId, int page, int size) {

        // Si no hay clubId, no tiene sentido buscar
        if (clubId == null) {
            return List.of();
        }

        final int limit = Math.max(0, size);
        if (limit == 0) {
            return List.of();
        }

        final int safePage = Math.max(0, page);
        final int offset   = safePage * limit;

        LocalDate today = LocalDate.now();

        return em.createQuery("""
                    SELECT e FROM Event e
                    WHERE e.clubId = :clubId
                      AND e.date < :today
                    ORDER BY e.date DESC, e.eventId DESC
                    """, Event.class)
                .setParameter("clubId", clubId)
                .setParameter("today", today)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }


    @Override
    public int countUpcomingByClub(UUID clubId) {
        return em.createQuery("""
                        SELECT COUNT(e)
                        FROM Event e
                        WHERE e.clubId = :clubId
                          AND e.date >= :today
                        """, Long.class)
                .setParameter("clubId", clubId)
                .setParameter("today", LocalDate.now())
                .getSingleResult()
                .intValue();
    }

    @Override
    public int countPastByClub(UUID clubId) {
        return em.createQuery("""
                        SELECT COUNT(e)
                        FROM Event e
                        WHERE e.clubId = :clubId
                          AND e.date < :today
                        """, Long.class)
                .setParameter("clubId", clubId)
                .setParameter("today", LocalDate.now())
                .getSingleResult()
                .intValue();
    }

    @Override
    public Map<UUID, String> mapClubNamesForEvents(List<UUID> eventIds) {
        if (eventIds == null || eventIds.isEmpty())
            return Collections.emptyMap();

        List<Object[]> rows = em.createQuery("""
                    SELECT e.eventId, c.clubName
                    FROM Event e, Club c
                    WHERE e.clubId = c.clubId
                      AND e.eventId IN :eventIds
                """, Object[].class)
                .setParameter("eventIds", eventIds)
                .getResultList();

        Map<UUID, String> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (String) row[1]);
        }

        return result;
    }
}
