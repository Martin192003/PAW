package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.Status;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class ClubTablesJpaDao implements ClubTablesDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public ClubTable create(int number, int capacity, UUID clubId, TableState tableState,
                            BigDecimal price, BigDecimal discount) {

        ClubTable t = new ClubTable();
        t.setTableId(UUID.randomUUID());
        t.setClubId(clubId);
        t.setTableNumber(number);
        t.setCapacity(capacity);
        t.setTableState(tableState);
        t.setPrice(price);
        t.setDiscount(discount);
        em.persist(t);
        return t;
    }

    @Override
    @Transactional
    public ClubTable update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity,
                        TableState state, BigDecimal price, BigDecimal discount) {

        ClubTable t = em.find(ClubTable.class, tableId);
        if (t == null) return null;

        t.setClubId(clubId);
        if (tableNumber != null) t.setTableNumber(tableNumber);
        if (capacity != null) t.setCapacity(capacity);
        if (state != null) t.setTableState(state);
        if (price != null) t.setPrice(price);
        if (discount != null) t.setDiscount(discount);

        return t;
    }

    @Override
    public List<ClubTable> findAvailableByClubAndRange(final UUID clubId,
                                                       final OffsetDateTime from,
                                                       final OffsetDateTime to) {
        final int total = countAvailableByClubAndRange(clubId, from, to);
        if (total == 0) return List.of();
        return findAvailableByClubAndRangePaginated(clubId, from, to, total, 0);
    }


    @Override
    public List<ClubTable> findAvailableByClubAndRangePaginated(final UUID clubId,
                                                                final OffsetDateTime from,
                                                                final OffsetDateTime to,
                                                                final int limit,
                                                                final int offset) {
        // 1) IDs paginados (SQL nativo) — ¡OJO! bind sobre NativeQuery y setParameterList
        final String idSql = """
        SELECT CAST(ct.table_id AS TEXT) AS id_text
        FROM club_tables ct
        WHERE ct.club_id = :clubId
          AND NOT EXISTS (
              SELECT 1
              FROM bookings b
              WHERE b.table_id = ct.table_id
                AND LOWER(b.status) IN (:blocking)
                AND b.booking_start < :to_ts
                AND b.booking_end   > :from_ts
          )
        ORDER BY ct.table_number ASC, ct.table_id ASC
        LIMIT :limit OFFSET :offset
    """;

        final java.util.List<String> blocking =
                java.util.List.of(Status.PENDIENTE.db().toLowerCase(), Status.CONFIRMADA.db().toLowerCase());

        final org.hibernate.query.NativeQuery<String> nq = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING);

        nq.setParameter("clubId", clubId);
        nq.setParameterList("blocking", blocking);
        nq.setParameter("from_ts", java.sql.Timestamp.from(from.toInstant()));
        nq.setParameter("to_ts",   java.sql.Timestamp.from(to.toInstant()));
        nq.setParameter("limit",   limit);
        nq.setParameter("offset",  offset);

        @SuppressWarnings("unchecked")
        final java.util.List<String> idTexts = nq.getResultList();
        if (idTexts.isEmpty()) return java.util.List.of();

        final java.util.List<java.util.UUID> ids = idTexts.stream()
                .map(java.util.UUID::fromString)
                .toList();

        // 2) Entidades (JPQL) — usa el nombre del CAMPO: ct.tableid (no tableId)
        return em.createQuery("""
            SELECT ct
            FROM ClubTable ct
            WHERE ct.tableid IN :ids
            ORDER BY ct.tableNumber ASC, ct.tableid ASC
        """, ClubTable.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public int countAvailableByClubAndRange(final UUID clubId,
                                            final OffsetDateTime from,
                                            final OffsetDateTime to) {
        final String countSql = """
        SELECT COUNT(*)
        FROM club_tables ct
        WHERE ct.club_id = :clubId
          AND NOT EXISTS (
              SELECT 1
              FROM bookings b
              WHERE b.table_id = ct.table_id
                AND LOWER(b.status) IN (:blocking)
                AND b.booking_start < :to_ts
                AND b.booking_end   > :from_ts
          )
    """;

        final java.util.List<String> blocking =
                java.util.List.of(Status.PENDIENTE.db().toLowerCase(), Status.CONFIRMADA.db().toLowerCase());

        final org.hibernate.query.NativeQuery<?> nq = em.createNativeQuery(countSql)
                .unwrap(org.hibernate.query.NativeQuery.class);

        nq.setParameter("clubId", clubId);
        nq.setParameterList("blocking", blocking);
        nq.setParameter("from_ts", java.sql.Timestamp.from(from.toInstant()));
        nq.setParameter("to_ts",   java.sql.Timestamp.from(to.toInstant()));

        final Number n = (Number) nq.getSingleResult();
        return n.intValue();
    }


    @Override
    public boolean delete(UUID id) {
        ClubTable t = em.find(ClubTable.class, id);
        if (t == null) return false;
        em.remove(t);
        return true;
    }

    @Override
    public Optional<ClubTable> findById(UUID id) {
        return Optional.ofNullable(em.find(ClubTable.class, id));
    }


    @Override
    public List<ClubTable> findByClub(Club club) {
        return em.createQuery("""
                SELECT ct
                FROM ClubTable ct
                WHERE ct.clubId = :cid
                ORDER BY ct.tableNumber ASC
                """, ClubTable.class)
                .setParameter("cid", club.getClubId())
                .getResultList();
    }
}
