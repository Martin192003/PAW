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

        // 1) Paginamos solo IDs
        @SuppressWarnings("unchecked")
        List<Object> rawIds = em.createNativeQuery("""
                    SELECT CAST(eventid AS TEXT)
                    FROM event
                    WHERE clubid = :clubId
                    ORDER BY date DESC, eventid DESC
                    """)
                .setParameter("clubId", clubId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (rawIds.isEmpty()) {
            return List.of();
        }

        // Convertimos los IDs (String) a UUID
        List<UUID> ids = rawIds.stream()
                .map(o -> UUID.fromString(o.toString()))
                .toList();

        // 2) Traemos las entidades por ID (JPQL)
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

        // 1) IDs futuros desde fromInclusive
        @SuppressWarnings("unchecked")
        List<Object> rawIds = em.createNativeQuery("""
            SELECT CAST(eventid AS TEXT)
            FROM event
            WHERE date >= :from
            ORDER BY date ASC, eventid ASC
            """)
                .setParameter("from", fromInclusive)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (rawIds.isEmpty()) {
            return List.of();
        }

        // Convertimos los IDs (String) a UUID
        List<UUID> ids = rawIds.stream()
                .map(o -> UUID.fromString(o.toString()))
                .toList();

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

        // 1) Query de IDs
        @SuppressWarnings("unchecked")
        List<Object> rawIds = em.createNativeQuery("""
            SELECT CAST(eventid AS TEXT)
            FROM event
            WHERE clubid = :clubId
              AND date >= :today
            ORDER BY date ASC, eventid ASC
            """)
                .setParameter("clubId", clubId)
                .setParameter("today", today)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (rawIds.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = rawIds.stream()
                .map(o -> UUID.fromString(o.toString()))
                .toList();

        // 2) Entidades
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

        // 1) Query de IDs
        @SuppressWarnings("unchecked")
        List<Object> rawIds = em.createNativeQuery("""
            SELECT CAST(eventid AS TEXT)
            FROM event
            WHERE clubid = :clubId
              AND date < :today
            ORDER BY date DESC, eventid DESC
            """)
                .setParameter("clubId", clubId)
                .setParameter("today", today)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        if (rawIds.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = rawIds.stream()
                .map(o -> UUID.fromString(o.toString()))
                .toList();

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
