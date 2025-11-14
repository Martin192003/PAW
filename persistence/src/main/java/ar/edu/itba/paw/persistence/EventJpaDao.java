package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Event;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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

        // 1) Paginamos solo IDs
        List<UUID> ids = em.createQuery("""
                SELECT e.eventId
                FROM Event e
                WHERE e.clubId = :clubId
                ORDER BY e.date DESC, e.eventId DESC
                """, UUID.class)
                .setParameter("clubId", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        // 2) Traemos las entidades por ID
        return em.createQuery("""
                SELECT e
                FROM Event e
                WHERE e.eventId IN :ids
                ORDER BY e.date DESC, e.eventId DESC
                """, Event.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public List<Event> searchByDateRangePaginated(final LocalDate startDate,
                                                  final LocalDate endDate,
                                                  final int page,
                                                  final int size) {
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        if (limit == 0) {
            return List.of();
        }

        // 1) IDs por rango de fechas
        List<UUID> ids = em.createQuery("""
                SELECT e.eventId
                FROM Event e
                WHERE e.date BETWEEN :start AND :end
                ORDER BY e.date ASC, e.eventId ASC
                """, UUID.class)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        // 2) Entidades por IDs
        return em.createQuery("""
                SELECT e
                FROM Event e
                WHERE e.eventId IN :ids
                ORDER BY e.date ASC, e.eventId ASC
                """, Event.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public int countByDateRange(LocalDate startDate, LocalDate endDate) {
        final Long total = em.createQuery(
                        "SELECT COUNT(e) FROM Event e WHERE e.date BETWEEN :start AND :end", Long.class)
                .setParameter("start", startDate)
                .setParameter("end", endDate)
                .getSingleResult();
        return total.intValue();
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
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        if (limit == 0) {
            return List.of();
        }

        // 1) IDs futuros desde fromInclusive
        List<UUID> ids = em.createQuery("""
                SELECT e.eventId
                FROM Event e
                WHERE e.date >= :from
                ORDER BY e.date ASC, e.eventId ASC
                """, UUID.class)
                .setParameter("from", fromInclusive)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        // 2) Entidades por IDs
        return em.createQuery("""
                SELECT e
                FROM Event e
                WHERE e.eventId IN :ids
                ORDER BY e.date ASC, e.eventId ASC
                """, Event.class)
                .setParameter("ids", ids)
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
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        if (limit == 0) {
            return List.of();
        }

        // 1) IDs futuros de un club
        List<UUID> ids = em.createQuery("""
                SELECT e.eventId
                FROM Event e
                WHERE e.clubId = :clubId
                  AND e.date >= :today
                ORDER BY e.date ASC, e.eventId ASC
                """, UUID.class)
                .setParameter("clubId", clubId)
                .setParameter("today", LocalDate.now())
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        // 2) Entidades por IDs
        return em.createQuery("""
                SELECT e
                FROM Event e
                WHERE e.eventId IN :ids
                ORDER BY e.date ASC, e.eventId ASC
                """, Event.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public List<Event> findPastByClub(UUID clubId, int page, int size) {
        final int limit  = Math.max(0, size);
        final int offset = Math.max(0, page) * limit;

        if (limit == 0) {
            return List.of();
        }

        // 1) IDs pasados de un club
        List<UUID> ids = em.createQuery("""
                SELECT e.eventId
                FROM Event e
                WHERE e.clubId = :clubId
                  AND e.date < :today
                ORDER BY e.date DESC, e.eventId DESC
                """, UUID.class)
                .setParameter("clubId", clubId)
                .setParameter("today", LocalDate.now())
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        // 2) Entidades por IDs
        return em.createQuery("""
                SELECT e
                FROM Event e
                WHERE e.eventId IN :ids
                ORDER BY e.date DESC, e.eventId DESC
                """, Event.class)
                .setParameter("ids", ids)
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
}
