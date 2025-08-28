package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Activity;
import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.User;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ActivityDao {
    Activity findById(UUID id);
    List<Activity> findAll();
    List<Activity> findByUser(User user);

    Activity create(Booking booking, String activityType, String details, OffsetDateTime activityDate);
    Activity update(Activity activity);
    boolean delete(UUID activityId);
}
