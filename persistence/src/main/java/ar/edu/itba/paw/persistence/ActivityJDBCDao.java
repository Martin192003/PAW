package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Activity;
import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class ActivityJDBCDao implements ActivityDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<Activity> ROW_MAPPER = (rs, rowNum) -> {
        UUID id = rs.getObject("activity_id", UUID.class);
        UUID bookingId = rs.getObject("booking_id", UUID.class);
        String type = rs.getString("activity_type");
        OffsetDateTime date = rs.getObject("activity_date", OffsetDateTime.class);
        String details = rs.getString("details");
        // booking se completa en el método a partir de bookingDao para evitar N+1 si hace falta
        Activity a = new Activity();
        a.setActivityId(id);
        a.setActivityType(type);
        a.setActivityDate(date);
        a.setDetails(details);

        // set bookingId temporalmente usando un booking "liviano"
        Booking b = new Booking();
        b.setBookingId(bookingId);
        a.setBooking(b);
        return a;
    };

    @Autowired
    public ActivityJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("activity")
                .usingColumns("activity_id", "booking_id", "activity_type", "activity_date", "details");
    }

    @Override
    public Activity create(Booking booking, String activityType, String details, OffsetDateTime activityDate) {
        UUID id = UUID.randomUUID();
        HashMap<String,Object> params = new HashMap<>();
        params.put("activity_id", id);
        params.put("booking_id", booking.getBookingId());
        params.put("activity_type", activityType);
        params.put("activity_date", activityDate);
        params.put("details", details);
        insert.execute(params);
        return new Activity(id, booking, activityType, activityDate, details);
    }

    @Override
    public Activity update(UUID id, Booking booking, String activityType, String details, OffsetDateTime activityDate) {
        final String sql = "UPDATE activity SET booking_id = ?, activity_type = ?, activity_date = ?, details = ? WHERE activity_id = ?";
        int upd = jdbc.update(sql,
                booking.getBookingId(),
                activityType,
                activityDate,
                details,
                id);
        return upd > 0 ? new Activity(id, booking, activityType, activityDate, details) : null;
    }

    @Override
    public boolean delete(UUID activityId) {
        final String sql = "DELETE FROM activity WHERE activity_id = ?";
        return jdbc.update(sql, activityId) > 0;
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        final String sql = "SELECT * FROM activity WHERE activity_id = ?";
        Activity a = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        return Optional.ofNullable(a);
    }

    @Override
    public List<Activity> findAll() {
        final String sql = "SELECT * FROM activity";
        List<Activity> list = jdbc.query(sql, ROW_MAPPER);
        return list;
    }

    @Override
    public List<Activity> findByUser(User user) {
        final String sql =
                "SELECT a.* " +
                        "FROM activity a JOIN bookings b ON a.booking_id = b.booking_id " +
                        "WHERE b.user_id = ?";
        UUID userId = user.getId();
        List<Activity> list = jdbc.query(sql, ROW_MAPPER, userId);
        return list;
    }

}
