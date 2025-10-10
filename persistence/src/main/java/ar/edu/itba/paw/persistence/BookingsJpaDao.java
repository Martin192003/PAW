package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class BookingsJpaDao implements BookingDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Booking create(User user, UUID tableId, Status status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {

        Booking b = new Booking();
        b.setBookingId(UUID.randomUUID());
        b.setUser(user);

        ClubTable ref = em.getReference(ClubTable.class, tableId);
        b.setTable(ref);

        b.setBookingStart(bookingStart);
        b.setBookingEnd(bookingEnd);
        b.setStatus(status);
        b.setTotalAmount(totalAmount);
        b.setCreatedAt(OffsetDateTime.now());

        em.persist(b);
        return b;
    }

    @Override
    public Optional<Booking> update(UUID bookingId, User user, UUID tableId, Status status,
                                    BigDecimal totalAmount, OffsetDateTime bookingStart,
                                    OffsetDateTime bookingEnd) {
        Booking b = em.find(Booking.class, bookingId);
        if (b == null) return Optional.empty();

        b.setUser(user);

        ClubTable ref = em.getReference(ClubTable.class, tableId);
        b.setTable(ref);

        b.setStatus(status);
        b.setTotalAmount(totalAmount);
        b.setBookingStart(bookingStart);
        b.setBookingEnd(bookingEnd);
        return Optional.of(b);
    }

    @Override
     public boolean delete(UUID bookingId) {
        Booking b = em.find(Booking.class, bookingId);
        if (b == null) return false;
        em.remove(b);
        return true;
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return Optional.ofNullable(em.find(Booking.class, id));
    }

    @Override
     public Optional<Booking> updateStatus(UUID bookingId, Status newStatus) {
        Booking b = em.find(Booking.class, bookingId);
        if (b == null) return Optional.empty();
        b.setStatus(newStatus);
        return Optional.of(b);
    }


    @Override
    public List<Booking> findAll() {
        return em.createQuery("SELECT b FROM Booking b", Booking.class)
                .getResultList();
    }

    @Override
    public List<Booking> findByUser(UUID userId) {
        return em.createQuery("""
                SELECT b
                FROM Booking b
                WHERE b.user.id = :uid
                """, Booking.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    @Override
    public List<Booking> findByTable(UUID tableId) {
        return em.createQuery("""
            SELECT b
            FROM Booking b
            WHERE b.table.id = :tid
            """, Booking.class)
                .setParameter("tid", tableId) // ahora sí, UUID
                .getResultList();
    }

    @Override
    public List<Booking> findAllPaginated(int limit, int offset) {
        final String orderBy = resolveOrderBy("created_desc", false);
        return em.createQuery("SELECT b FROM Booking b ORDER BY " + orderBy, Booking.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public int countAll() {
        Long cnt = em.createQuery("SELECT COUNT(b) FROM Booking b", Long.class)
                .getSingleResult();
        return cnt.intValue();
    }


    @Override
    public List<Booking> searchPaginated(UUID userId,
                                         UUID clubId,
                                         UUID bookingId,
                                         Status status,
                                         String sort,
                                         int limit,
                                         int offset) {

        if (clubId != null) {
            String sql = "SELECT b.* FROM bookings b " +
                    "LEFT JOIN club_tables t ON t.table_id = b.table_id " +
                    "WHERE t.club_id = :clubId";

            if (userId != null)   sql += " AND b.user_id = :userId";
            if (bookingId != null) sql += " AND b.booking_id = :bookingId";
            if (status != null)    sql += " AND LOWER(b.status) = LOWER(:status)";

            sql += " ORDER BY " + resolveOrderBy(sort, true) + " LIMIT :limit OFFSET :offset";

            Query q = em.createNativeQuery(sql, Booking.class)
                    .setParameter("clubId", clubId)
                    .setParameter("limit", limit)
                    .setParameter("offset", offset);

            if (userId != null)    q.setParameter("userId", userId);
            if (bookingId != null) q.setParameter("bookingId", bookingId);
            if (status != null)    q.setParameter("status", status.db());

            @SuppressWarnings("unchecked")
            List<Booking> res = q.getResultList();
            return res;
        }

        StringBuilder jpql = new StringBuilder("SELECT b FROM Booking b WHERE 1=1");
        Map<String,Object> params = new HashMap<>();
        if (userId != null)   { jpql.append(" AND b.user.id = :userId"); params.put("userId", userId); }
        if (bookingId != null){ jpql.append(" AND b.bookingId = :bookingId"); params.put("bookingId", bookingId); }
        if (status != null)   { jpql.append(" AND b.status = :status"); params.put("status", status); }

        jpql.append(" ORDER BY ").append(resolveOrderBy(sort, false));

        TypedQuery<Booking> q = em.createQuery(jpql.toString(), Booking.class)
                .setFirstResult(offset)
                .setMaxResults(limit);
        params.forEach(q::setParameter);
        return q.getResultList();
    }

    @Override
    public int countSearch(UUID userId,
                           UUID clubId,
                           UUID bookingId,
                           Status status) {

        if (clubId != null) {
            String sql = "SELECT COUNT(*) FROM bookings b " +
                    "LEFT JOIN club_tables t ON t.table_id = b.table_id " +
                    "WHERE t.club_id = :clubId";

            if (userId != null)    sql += " AND b.user_id = :userId";
            if (bookingId != null) sql += " AND b.booking_id = :bookingId";
            if (status != null)    sql += " AND LOWER(b.status) = LOWER(:status)";

            Query q = em.createNativeQuery(sql)
                    .setParameter("clubId", clubId);

            if (userId != null)    q.setParameter("userId", userId);
            if (bookingId != null) q.setParameter("bookingId", bookingId);
            if (status != null)    q.setParameter("status", status.db());

            Number n = (Number) q.getSingleResult();
            return n.intValue();
        }

        StringBuilder jpql = new StringBuilder("SELECT COUNT(b) FROM Booking b WHERE 1=1");
        Map<String,Object> params = new HashMap<>();
        if (userId != null)    { jpql.append(" AND b.user.id = :userId"); params.put("userId", userId); }
        if (bookingId != null) { jpql.append(" AND b.bookingId = :bookingId"); params.put("bookingId", bookingId); }
        if (status != null)    { jpql.append(" AND b.status = :status"); params.put("status", status); }

        TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class);
        params.forEach(q::setParameter);
        return q.getSingleResult().intValue();
    }

    @Override
    public List<Booking> searchByClubIdsPaginated(List<UUID> clubIds,
                                                  UUID bookingId,
                                                  Status status,
                                                  String sort,
                                                  int limit,
                                                  int offset) {
        if (clubIds == null || clubIds.isEmpty()) return List.of();

        String sql = "SELECT b.* FROM bookings b " +
                "JOIN club_tables t ON t.table_id = b.table_id " +
                "WHERE t.club_id IN (:clubIds)";

        if (bookingId != null) sql += " AND b.booking_id = :bookingId";
        if (status != null)    sql += " AND LOWER(b.status) = LOWER(:status)";

        sql += " ORDER BY " + resolveOrderBy(sort, true) + " LIMIT :limit OFFSET :offset";

        Query q = em.createNativeQuery(sql, Booking.class)
                .setParameter("clubIds", clubIds)
                .setParameter("limit", limit)
                .setParameter("offset", offset);

        if (bookingId != null) q.setParameter("bookingId", bookingId);
        if (status != null)    q.setParameter("status", status.db());

        @SuppressWarnings("unchecked")
        List<Booking> res = q.getResultList();
        return res;
    }

    @Override
    public int countByClubIds(List<UUID> clubIds, UUID bookingId, Status status) {
        if (clubIds == null || clubIds.isEmpty()) return 0;

        String sql = "SELECT COUNT(*) FROM bookings b " +
                "JOIN club_tables t ON t.table_id = b.table_id " +
                "WHERE t.club_id IN (:clubIds)";

        if (bookingId != null) sql += " AND b.booking_id = :bookingId";
        if (status != null)    sql += " AND LOWER(b.status) = LOWER(:status)";

        Query q = em.createNativeQuery(sql)
                .setParameter("clubIds", clubIds);

        if (bookingId != null) q.setParameter("bookingId", bookingId);
        if (status != null)    q.setParameter("status", status.db());

        Number n = (Number) q.getSingleResult();
        return n.intValue();
    }


    private static String resolveOrderBy(String sort, boolean nativeSql) {
        return switch (sort == null ? "" : sort) {
            case "created_asc" -> nativeSql ? "b.created_at ASC NULLS LAST"
                    : "(CASE WHEN b.createdAt IS NULL THEN 1 ELSE 0 END) ASC, b.createdAt ASC";
            case "start_desc" -> nativeSql ? "b.booking_start DESC"
                    : "b.bookingStart DESC";
            case "start_asc" -> nativeSql ? "b.booking_start ASC"
                    : "b.bookingStart ASC";
            case "status_asc" -> "b.status ASC";
            default -> // created_desc
                    nativeSql ? "b.created_at DESC NULLS LAST"
                            : "(CASE WHEN b.createdAt IS NULL THEN 1 ELSE 0 END) ASC, b.createdAt DESC";
        };
    }

}
