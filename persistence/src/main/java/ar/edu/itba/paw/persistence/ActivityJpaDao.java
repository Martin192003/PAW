package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Activity;
import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class ActivityJpaDao implements ActivityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Activity create(Booking booking, String activityType, String details, OffsetDateTime activityDate) {
        final Activity a = new Activity();
        a.setBooking(booking);
        a.setActivityType(activityType);
        a.setActivityDate(activityDate);
        a.setDetails(details);
        em.persist(a);
        return a;
    }

    @Override
    public Activity update(UUID id, Booking booking, String activityType, String details, OffsetDateTime activityDate) {
        Activity a = em.find(Activity.class, id);
        if (a == null) return null;
        a.setBooking(booking);
        a.setActivityType(activityType);
        a.setActivityDate(activityDate);
        a.setDetails(details);
        return a;
    }

    @Override
    public boolean delete(UUID activityId) {
        Activity a = em.find(Activity.class, activityId);
        if (a == null) return false;
        em.remove(a);
        return true;
    }

    @Override
    public Optional<Activity> findById(UUID id) {
        return Optional.ofNullable(em.find(Activity.class, id));
    }

    @Override
    public List<Activity> findAll() {
        return em.createQuery("SELECT a FROM Activity a", Activity.class)
                .getResultList();
    }

    @Override
    public List<Activity> findByUser(User user) {
        return em.createQuery("""
                SELECT a
                FROM Activity a
                JOIN a.booking b
                WHERE b.user.id = :uid
                """, Activity.class)
                .setParameter("uid", user.getId())
                .getResultList();
    }
}
