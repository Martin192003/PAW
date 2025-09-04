package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.Status;
import ar.edu.itba.paw.model.Table;
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
        params.put("booking_start", bookingStart);
        params.put("booking_end", bookingEnd);
        params.put("status", status.db());
        params.put("total_amount", totalAmount);

        insert.execute(params);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted booking not found: " + id));
    }


    @Override
    public Booking update(UUID bookingId, User user, UUID tableId, Status status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {
        final String sql = "UPDATE bookings " +
                "SET user_id = ?, table_id = ?, booking_start = ?, booking_end = ?, status = ?, total_amount = ? " +
                "WHERE booking_id = ?";

        int updated = jdbc.update(sql,
                user.getId(),
                tableId,
                bookingStart,
                bookingEnd,
                status.db(),      // "pendiente" | "disponible" | "reservada"
                totalAmount,
                bookingId
        );

        return updated > 0 ? findById(bookingId).orElse(null) : null;
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
    public List<Booking> findAll() {
        final String sql = "SELECT * FROM bookings";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Booking> findByUser(User user) {
        final String sql = "SELECT * FROM bookings WHERE user_id = ?";
        return jdbc.query(sql, ROW_MAPPER, user.getId());
    }

    @Override
    public List<Booking> findByTable(UUID tableId) {
        final String sql = "SELECT * FROM bookings WHERE table_id = ?";
        return jdbc.query(sql, ROW_MAPPER, tableId);
    }

}
