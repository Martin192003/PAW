package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Repository;


import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class BookingsJpaDao implements BookingDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Booking create(User user, UUID tableId, Status status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {

        if (bookingEnd != null && bookingStart != null && !bookingEnd.isAfter(bookingStart)) {
            throw new PersistenceException("booking_end must be greater than booking_start");
        }

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
    public List<Booking> searchByClubIdsPaginated(final List<UUID> clubIds,
                                                  final UUID bookingId,
                                                  final Status status,
                                                  final String sort,
                                                  final LocalDate bookingDate,
                                                  final Boolean hasReceipt, // NEW
                                                  final int limit,
                                                  final int offset) {
        if (clubIds == null || clubIds.isEmpty()) return List.of();

        final String ph = placeholders("cid", clubIds.size());

        final StringBuilder inner = new StringBuilder("""
                SELECT DISTINCT b.booking_id, b.created_at, b.booking_start, b.status
                FROM bookings b
                JOIN club_tables t ON t.table_id = b.table_id
                """);

        if (hasReceipt != null) {
            inner.append("LEFT JOIN receipts r ON r.booking_id = b.booking_id\n");
        }

        inner.append("WHERE t.club_id IN ( %s )\n".formatted(ph));

        if (bookingId != null) inner.append("AND b.booking_id = :bookingId\n");
        if (status != null) inner.append("AND LOWER(b.status) = LOWER(:status)\n");
        if (bookingDate != null) inner.append("AND CAST(b.booking_start AS DATE) = :bookingDate\n");
        if (hasReceipt != null) {
            if (hasReceipt) inner.append("AND r.booking_id IS NOT NULL\n");
            else inner.append("AND r.booking_id IS NULL\n");
        }

        final String idSql = """
                    SELECT CAST(x.booking_id AS TEXT) AS id_text
                    FROM ( %s ) x
                    ORDER BY %s
                """.formatted(inner, nativeSortForAlias("x", sort));

        final var nq = em.createNativeQuery(idSql)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setFirstResult(offset)
                .setMaxResults(limit);

        bindUuidList(nq, "cid", clubIds);
        if (bookingId != null) nq.setParameter("bookingId", bookingId);
        if (status != null) nq.setParameter("status", status.db());
        if (bookingDate != null) nq.setParameter("bookingDate", bookingDate);


        @SuppressWarnings("unchecked") final List<String> idTexts = nq.getResultList();
        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream()
                .map(UUID::fromString)
                .toList();

        final String orderBy = resolveOrderBy(sort, false);
        return em.createQuery(
                "SELECT b FROM Booking b WHERE b.bookingId IN :ids ORDER BY " + orderBy,
                Booking.class
        ).setParameter("ids", ids).getResultList();
    }

    @Override
    public List<Booking> searchPaginated(final UUID userId,
                                         final UUID clubId,
                                         final UUID bookingId,
                                         final Status status,
                                         final String sort,
                                         final LocalDate bookingDate,
                                         final Boolean hasReceipt, // NEW
                                         final int limit,
                                         final int offset) {

        final StringBuilder inner = new StringBuilder("""
                SELECT DISTINCT b.booking_id, b.created_at, b.booking_start, b.status
                FROM bookings b
                """);

        if (clubId != null) {
            inner.append("LEFT JOIN club_tables t ON t.table_id = b.table_id\n");
        }
        if (hasReceipt != null) {
            inner.append("LEFT JOIN receipts r ON r.booking_id = b.booking_id\n");
        }

        inner.append("WHERE 1=1\n");
        if (clubId != null) inner.append("AND t.club_id = :clubId\n");
        if (userId != null) inner.append("AND b.user_id = :userId\n");
        if (bookingId != null) inner.append("AND b.booking_id = :bookingId\n");
        if (status != null) inner.append("AND LOWER(b.status) = LOWER(:status)\n");
        if (bookingDate != null) inner.append("AND CAST(b.booking_start AS DATE) = :bookingDate\n");
        if (hasReceipt != null) {
            if (hasReceipt) inner.append("AND r.booking_id IS NOT NULL\n");
            else inner.append("AND r.booking_id IS NULL\n");
        }

        final String idSql = """
                    SELECT CAST(x.booking_id AS TEXT) AS id_text
                    FROM ( %s ) x
                    ORDER BY %s
                """.formatted(inner, nativeSortForAlias("x", sort));

        final var nq = em.createNativeQuery(idSql)
                .unwrap(NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (clubId != null) nq.setParameter("clubId", clubId);
        if (userId != null) nq.setParameter("userId", userId);
        if (bookingId != null) nq.setParameter("bookingId", bookingId);
        if (status != null) nq.setParameter("status", status.db());
        if (bookingDate != null) nq.setParameter("bookingDate", bookingDate);

        @SuppressWarnings("unchecked") final List<String> idTexts = nq.getResultList();
        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream()
                .map(UUID::fromString)
                .toList();

        final String orderBy = resolveOrderBy(sort, false);
        return em.createQuery(
                "SELECT b FROM Booking b WHERE b.bookingId IN :ids ORDER BY " + orderBy,
                Booking.class
        ).setParameter("ids", ids).getResultList();
    }


    @Override
    public int countByClubIds(final List<UUID> clubIds, final UUID bookingId, final Status status, final LocalDate bookingDate, final Boolean hasReceipt) {
        if (clubIds == null || clubIds.isEmpty()) return 0;

        final String ph = placeholders("cid", clubIds.size());

        final StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM bookings b
                JOIN club_tables t ON t.table_id = b.table_id
                """);
        if (hasReceipt != null) sql.append("LEFT JOIN receipts r ON r.booking_id = b.booking_id\n");
        sql.append("WHERE t.club_id IN ( %s )\n".formatted(ph));

        if (bookingId != null) sql.append("AND b.booking_id = :bookingId\n");
        if (status != null) sql.append("AND LOWER(b.status) = LOWER(:status)\n");
        if (bookingDate != null) sql.append(" AND CAST(b.booking_start AS DATE) = :bookingDate");
        if (hasReceipt != null) {
            if (hasReceipt) sql.append(" AND r.booking_id IS NOT NULL\n");
            else sql.append(" AND r.booking_id IS NULL\n");
        }

        final var nq = em.createNativeQuery(sql.toString()).unwrap(NativeQuery.class);

        bindUuidList(nq, "cid", clubIds);

        if (bookingId != null) nq.setParameter("bookingId", bookingId);
        if (status != null) nq.setParameter("status", status.db());
        if (bookingDate != null) nq.setParameter("bookingDate", bookingDate);

        final Number n = (Number) nq.getSingleResult();
        return n.intValue();
    }


    @Override
    public int countSearch(UUID userId,
                           UUID clubId,
                           UUID bookingId,
                           Status status,
                           LocalDate bookingDate,
                           Boolean hasReceipt) {

        if (clubId != null) {
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) FROM bookings b LEFT JOIN club_tables t ON t.table_id = b.table_id "
            );
            if (hasReceipt != null) {
                sql.append("LEFT JOIN receipts r ON r.booking_id = b.booking_id ");
            }
            sql.append("WHERE t.club_id = :clubId");

            if (userId != null)   sql.append(" AND b.user_id = :userId");
            if (bookingId != null) sql.append(" AND b.booking_id = :bookingId");
            if (status != null)   sql.append(" AND LOWER(b.status) = LOWER(:status)");
            if (bookingDate != null) sql.append(" AND CAST(b.booking_start AS DATE) = :bookingDate");
            if (hasReceipt != null) {
                if (hasReceipt) sql.append(" AND r.booking_id IS NOT NULL");
                else           sql.append(" AND r.booking_id IS NULL");
            }

            Query q = em.createNativeQuery(sql.toString())
                    .setParameter("clubId", clubId);

            if (userId != null)   q.setParameter("userId", userId);
            if (bookingId != null) q.setParameter("bookingId", bookingId);
            if (status != null)   q.setParameter("status", status.db());
            if (bookingDate != null) q.setParameter("bookingDate", bookingDate);

            Number n = (Number) q.getSingleResult();
            return n.intValue();
        }

        StringBuilder jpql = new StringBuilder("SELECT COUNT(b) FROM Booking b WHERE 1=1");
        Map<String, Object> params = new HashMap<>();

        if (userId != null) {
            jpql.append(" AND b.user.id = :userId");
            params.put("userId", userId);
        }
        if (bookingId != null) {
            jpql.append(" AND b.bookingId = :bookingId");
            params.put("bookingId", bookingId);
        }
        if (status != null) {
            jpql.append(" AND b.status = :status");
            params.put("status", status);
        }

        if (hasReceipt != null) {
            StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM bookings b ");
            sql.append("LEFT JOIN receipts r ON r.booking_id = b.booking_id WHERE 1=1 ");
            if (userId != null)   sql.append(" AND b.user_id = :userId");
            if (bookingId != null) sql.append(" AND b.booking_id = :bookingId");
            if (status != null)   sql.append(" AND LOWER(b.status) = LOWER(:status)");
            if (bookingDate != null) sql.append(" AND CAST(b.booking_start AS DATE) = :bookingDate");
            if (hasReceipt) sql.append(" AND r.booking_id IS NOT NULL");
            else            sql.append(" AND r.booking_id IS NULL");

            Query q = em.createNativeQuery(sql.toString());
            if (userId != null)   q.setParameter("userId", userId);
            if (bookingId != null) q.setParameter("bookingId", bookingId);
            if (status != null)   q.setParameter("status", status.db());
            if (bookingDate != null) q.setParameter("bookingDate", bookingDate);

            Number n = (Number) q.getSingleResult();
            return n.intValue();
        }

        TypedQuery<Long> q = em.createQuery(jpql.toString(), Long.class);
        params.forEach(q::setParameter);
        return q.getSingleResult().intValue();
    }


    @Override
    public boolean existsPastConfirmedByUserAndClub(UUID userId, UUID clubId, OffsetDateTime now) {
        final Number oneOrZero = em.createNativeQuery("""
                        SELECT 1
                        FROM bookings b
                        JOIN club_tables t ON t.table_id = b.table_id
                        WHERE b.user_id = :uid
                          AND t.club_id = :cid
                          AND b.status = :confirmed
                          AND b.booking_end < :now
                        LIMIT 1
                        """)
                .setParameter("uid", userId)
                .setParameter("cid", clubId)
                .setParameter("confirmed", Status.CONFIRMADA.db())
                .setParameter("now", now).getResultList().size();

        return oneOrZero.longValue() > 0;
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

    private static String nativeSortForAlias(final String alias, final String sort) {
        final String a = alias == null ? "x" : alias;
        return switch (sort == null ? "" : sort) {
            case "created_asc" -> a + ".created_at ASC NULLS LAST, " + a + ".booking_id ASC";
            case "start_desc" -> a + ".booking_start DESC NULLS LAST, " + a + ".booking_id DESC";
            case "start_asc" -> a + ".booking_start ASC NULLS LAST, " + a + ".booking_id ASC";
            case "status_asc" -> a + ".status ASC NULLS LAST, " + a + ".booking_id ASC";
            default /* created_desc */ -> a + ".created_at DESC NULLS LAST, " + a + ".booking_id DESC";
        };
    }


    @Override
    public Map<UUID, String> mapClubNamesForBookings(List<UUID> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty())
            return Collections.emptyMap();

        String placeholders = "";
        for (int i = 0; i < bookingIds.size(); i++) {
            placeholders += (i == 0 ? ":" : ", :") + "id" + i;
        }

        String sql = """
                    SELECT CAST(b.booking_id AS TEXT) AS booking_id, c.club_name
                    FROM bookings b
                    JOIN club_tables t ON b.table_id = t.table_id
                    JOIN clubs c ON t.club_id = c.club_id
                    WHERE b.booking_id IN (%s)
                """.formatted(placeholders);

        var nq = em.createNativeQuery(sql);

        for (int i = 0; i < bookingIds.size(); i++) {
            nq.setParameter("id" + i, bookingIds.get(i));
        }

        List<Object[]> rows = nq.getResultList();
        Map<UUID, String> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put(UUID.fromString(row[0].toString()), (String) row[1]);
        }

        return result;
    }



    private static String placeholders(final String base, final int n) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(", ");
            sb.append(':').append(base).append(i);
        }
        return sb.toString();
    }

    private static void bindUuidList(final NativeQuery<?> nq, final String base, final List<UUID> values) {
        for (int i = 0; i < values.size(); i++) {
            nq.setParameter(base + i, values.get(i));
        }
    }
}
