package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class BookingsJDBCDao implements BookingDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final UserDao userDao;

    private static final RowMapper<Booking> ROW_MAPPER = (rs, rowNum) -> {
        Booking b = new Booking();
        b.setBookingId(rs.getObject("booking_id", UUID.class));

        UUID userId = rs.getObject("user_id", UUID.class);
        User u = new User();
        u.setUserId(userId);
        b.setUser(u);

        b.setTableId(rs.getObject("table_id", UUID.class));

        b.setBookingStart(rs.getObject("booking_start", OffsetDateTime.class));
        b.setBookingEnd(rs.getObject("booking_end", OffsetDateTime.class));

        b.setStatus(Status.fromDb(rs.getString("status")));

        b.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        return b;
    };
    @Autowired
    public BookingsJDBCDao(final DataSource ds, final UserDao userDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("bookings")
                .usingColumns("booking_id", "user_id", "table_id", "booking_start", "booking_end", "status", "total_amount");
        this.userDao = userDao;
    }

    @Override
    public Booking create(User user, UUID tableId, Status status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {

        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("booking_id", id);
        params.put("user_id", user.getId());
        params.put("table_id", tableId);
        params.put("booking_start", java.sql.Timestamp.from(bookingStart.toInstant()));
        params.put("booking_end",   java.sql.Timestamp.from(bookingEnd.toInstant()));
        params.put("status", status.db());
        params.put("total_amount", totalAmount);

        insert.execute(params);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted booking not found: " + id));
    }


    @Override
    public Optional<Booking> update(UUID bookingId, User user, UUID tableId, Status status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {
        final String sql = "UPDATE bookings " +
                "SET user_id = ?, table_id = ?, booking_start = ?, booking_end = ?, status = ?, total_amount = ? " +
                "WHERE booking_id = ?";

        int updated = jdbc.update(sql,
                user.getId(),
                tableId,
                java.sql.Timestamp.from(bookingStart.toInstant()),
                java.sql.Timestamp.from(bookingEnd.toInstant()),
                status.db(),      // "pendiente" | "disponible" | "reservada"
                totalAmount,
                bookingId
        );

        return updated > 0 ? findById(bookingId) : Optional.empty();
    }


    @Override
    public boolean delete(UUID bookingId) {
        final String sql = "DELETE FROM bookings WHERE booking_id = ?";
        return jdbc.update(sql, bookingId) > 0;
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        final String sql = "SELECT * FROM bookings WHERE booking_id = ?";
        Booking b = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        return Optional.ofNullable(b);
    }

    @Override
    public Optional<Booking> updateStatus(UUID bookingId, Status newStatus){
        final String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        int updated = jdbc.update(sql, newStatus.db(), bookingId);
        return updated > 0 ? findById(bookingId) : Optional.empty();
    }


    @Override
    public List<Booking> findAll() {
        final String sql = "SELECT * FROM bookings";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Booking> findByUser(UUID userId) {
        final String sql = "SELECT * FROM bookings WHERE user_id = ?";
        return jdbc.query(sql, ROW_MAPPER, userId);
    }

    @Override
    public List<Booking> findByTable(UUID tableId) {
        final String sql = "SELECT * FROM bookings WHERE table_id = ?";
        return jdbc.query(sql, ROW_MAPPER, tableId);
    }

    @Override
    public List<Booking> findAllPaginated(int limit, int offset) {
        final String sql = "SELECT * FROM bookings ORDER BY created_at DESC NULLS LAST LIMIT ? OFFSET ?";
        return jdbc.query(sql, ROW_MAPPER, limit, offset);
    }

    @Override
    public int countAll() {
        final String sql = "SELECT COUNT(*) FROM bookings";
        return jdbc.queryForObject(sql, Integer.class);
    }

    private static String resolveOrderBy(String sort) {
        // Whitelist para evitar SQL injection
        return switch (sort == null ? "" : sort) {
            case "created_asc" -> "b.created_at ASC NULLS LAST";
            case "start_desc"  -> "b.booking_start DESC";
            case "start_asc"   -> "b.booking_start ASC";
            case "status_asc"  -> "b.status ASC";
            default             -> "b.created_at DESC NULLS LAST"; // created_desc (default)
        };
    }

    @Override
    public List<Booking> searchPaginated(UUID userId,
                                         UUID clubId,
                                         UUID bookingId,
                                         Status status,
                                         String sort,
                                         int limit,
                                         int offset) {

        StringBuilder sql = new StringBuilder(
                "SELECT b.* " +
                        "FROM bookings b " +
                        // JOIN opcional para filtrar por club:
                        "LEFT JOIN club_tables t ON t.table_id = b.table_id " +
                        "WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();

        if (userId != null) {
            sql.append(" AND b.user_id = ?");
            args.add(userId);
        }
        if (clubId != null) {
            sql.append(" AND t.club_id = ?");
            args.add(clubId);
        }
        if (bookingId != null) {
            sql.append(" AND b.booking_id = ?");
            args.add(bookingId);
        }
        if (status != null) {
            sql.append(" AND LOWER(b.status) = LOWER(?)");
            args.add(status.db()); // en tu Status.db() devolvés "pendiente", "confirmada", etc.
        }

        sql.append(" ORDER BY ").append(resolveOrderBy(sort));
        sql.append(" LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);

        return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
    }

    @Override
    public int countSearch(UUID userId,
                           UUID clubId,
                           UUID bookingId,
                           Status status) {

        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) " +
                        "FROM bookings b " +
                        "LEFT JOIN club_tables t ON t.table_id = b.table_id " +
                        "WHERE 1=1"
        );
        List<Object> args = new ArrayList<>();

        if (userId != null) {
            sql.append(" AND b.user_id = ?");
            args.add(userId);
        }
        if (clubId != null) {
            sql.append(" AND t.club_id = ?");
            args.add(clubId);
        }
        if (bookingId != null) {
            sql.append(" AND b.booking_id = ?");
            args.add(bookingId);
        }
        if (status != null) {
            sql.append(" AND LOWER(b.status) = LOWER(?)");
            args.add(status.db());
        }

        return jdbc.queryForObject(sql.toString(), Integer.class, args.toArray());
    }



}
