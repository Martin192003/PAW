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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        b.setTableId(rs.getObject("table_id", Table.class));
        b.setBookingStart(rs.getObject("booking_start", OffsetDateTime.class));
        b.setBookingEnd(rs.getObject("booking_end", OffsetDateTime.class));
        b.setStatus(Status.valueOf(rs.getString("status")));
        b.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        return b;
    };

    @Autowired
    public BookingsJDBCDao(final DataSource ds, final UserDao userDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("bookings")
                .usingColumns("booking_id", "user_id", "table_id", "booking_start", "booking_end", "status", "total_amount", "created_at");
        this.userDao = userDao;
    }

    @Override
    public Booking create(User user, UUID tableId, String status, BigDecimal totalAmount,
                          OffsetDateTime bookingStart, OffsetDateTime bookingEnd) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("booking_id", id);
        params.put("user_id", user.getId());
        params.put("table_id", tableId);
        params.put("booking_start", bookingStart);
        params.put("booking_end", bookingEnd);
        params.put("status", status);
        params.put("total_amount", totalAmount);
        params.put("created_at", OffsetDateTime.now());
        insert.execute(params);
        return findById(id);
    }

    @Override
    public Booking update(Booking booking) {
        final String sql = "UPDATE bookings SET user_id=?, table_id=?, booking_start=?, booking_end=?, status=?, total_amount=? WHERE booking_id = ?";
        int updated = jdbc.update(sql,
                booking.getUser().getId(),
                booking.getTableId(),
                booking.getBookingStart(),
                booking.getBookingEnd(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getBookingId()
        );
        return updated > 0 ? findById(booking.getBookingId()) : null;
    }

    @Override
    public boolean delete(UUID bookingId) {
        final String sql = "DELETE FROM bookings WHERE booking_id = ?";
        return jdbc.update(sql, bookingId) > 0;
    }

    @Override
    public Booking findById(UUID id) {
        final String sql = "SELECT booking_id,user_id,table_id,booking_start,booking_end,status,created_at,total_amount FROM bookings WHERE booking_id = ?";
        Booking b = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        if (b != null) {
            b.setUser(userDao.findById(b.getUser().getId()));
        }
        return b;
    }

    @Override
    public List<Booking> findAll() {
        final String sql = "SELECT booking_id,user_id,table_id,booking_start,booking_end,status,created_at,total_amount FROM bookings";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Booking> findByUser(User user) {
        final String sql = "SELECT booking_id,user_id,table_id,booking_start,booking_end,status,created_at,total_amount FROM bookings WHERE user_id = ?";
        return jdbc.query(sql, ROW_MAPPER, user.getId());
    }

    @Override
    public List<Booking> findByTable(UUID tableId) {
        final String sql = "SELECT booking_id,user_id,table_id,booking_start,booking_end,status,created_at,total_amount FROM bookings WHERE table_id = ?";
        return jdbc.query(sql, ROW_MAPPER, tableId);
    }

}
