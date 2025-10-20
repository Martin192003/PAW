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

        // 1) Página de IDs (SQL nativo, orden estable + tie-break por PK)
        final String idSql = """
        SELECT CAST(e.event_id AS TEXT) AS id_text
        FROM events e
        WHERE e.club_id = :clubId
        ORDER BY e.date DESC, e.event_id DESC
        LIMIT :limit OFFSET :offset
    """;

        @SuppressWarnings("unchecked")
        final java.util.List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .setParameter("limit",  limit)
                .setParameter("offset", offset)
                .getResultList();

        if (idTexts.isEmpty()) return java.util.List.of();

        final java.util.List<java.util.UUID> ids = idTexts.stream()
                .map(java.util.UUID::fromString)
                .toList();

        // 2) Entidades por IDs (JPQL) con el mismo orden lógico
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

        // 1) Página de IDs
        final String idSql = """
        SELECT CAST(e.event_id AS TEXT) AS id_text
        FROM events e
        WHERE e.date BETWEEN :start AND :end
        ORDER BY e.date ASC, e.event_id ASC
        LIMIT :limit OFFSET :offset
    """;

        @SuppressWarnings("unchecked")
        final java.util.List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("start", startDate)
                .setParameter("end",   endDate)
                .setParameter("limit",  limit)
                .setParameter("offset", offset)
                .getResultList();

        if (idTexts.isEmpty()) return java.util.List.of();

        final java.util.List<java.util.UUID> ids = idTexts.stream()
                .map(java.util.UUID::fromString)
                .toList();

        // 2) Entidades por IDs (mismo orden)
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

        // 1) Página de IDs
        final String idSql = """
        SELECT CAST(e.event_id AS TEXT) AS id_text
        FROM events e
        WHERE e.date >= :from
        ORDER BY e.date ASC, e.event_id ASC
        LIMIT :limit OFFSET :offset
    """;

        @SuppressWarnings("unchecked")
        final java.util.List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("from", fromInclusive)
                .setParameter("limit",  limit)
                .setParameter("offset", offset)
                .getResultList();

        if (idTexts.isEmpty()) return java.util.List.of();

        final java.util.List<java.util.UUID> ids = idTexts.stream()
                .map(java.util.UUID::fromString)
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
}
