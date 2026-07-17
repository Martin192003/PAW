package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ReviewsJpaDaoTest {

    @Autowired
    private ReviewsJpaDao reviewsDao;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    private User user;
    private Review review;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Review r").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.createQuery("DELETE FROM User u").executeUpdate();
        em.flush();

        user = persistUser("u1");
        Club club = persistClub(user);

        review = persistReview(user, club);
        em.flush();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    /* ---------------------------------------------------------------------- */
    /*                                CREATE                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateBasic() {
        User u2 = persistUser("u2");
        Club c2 = persistClub(u2);

        Review r = reviewsDao.create(u2, c2, 5, "Excelente");
        em.flush();

        assertNotNull(r);
        assertEquals(u2.getId(), r.getUser().getId());
        assertEquals(c2.getClubId(), r.getClub().getClubId());
        assertEquals(Integer.valueOf(5), r.getRating());
        assertEquals("Excelente", r.getDescription());
        assertEquals(2L, countReviews());
    }

    @Test
    public void testCreateInvalidUserThrows() {
        Club c2 = persistClub(user);

        assertThrows(PersistenceException.class,
                () -> reviewsDao.create(null, c2, 5, "desc"));
    }

    @Test
    public void testCreateInvalidClubThrows() {
        User u2 = persistUser("uu");

        assertThrows(PersistenceException.class,
                () -> reviewsDao.create(u2, null, 5, "desc"));
    }

    /* ---------------------------------------------------------------------- */
    /*                                UPDATE                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testUpdateWorks() {
        UUID id = review.getReviewId();

        Review updated = reviewsDao.update(id, 1, "Bad review");
        em.flush();

        assertNotNull(updated);
        assertEquals(Integer.valueOf(1), updated.getRating());
        assertEquals("Bad review", updated.getDescription());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        Review r = reviewsDao.update(UUID.randomUUID(), 3, "Unknown");
        assertNull(r);
    }

    @Test
    public void testUpdateNullIdThrows() {
        assertThrows(NullPointerException.class,
                () -> reviewsDao.update(null, 3, "desc"));
    }

    @Test
    public void testUpdatePhotoIdWorksWithPendingPhotoInsert() {
        Photo photo = new Photo();
        photo.setClubId(review.getClub().getClubId());
        photo.setPostedBy(user.getId());
        photo.setFilename("review.jpg");
        photo.setContentType("image/jpeg");
        photo.setData(new byte[]{1, 2, 3});
        photo.setPhotoType(PhotoType.REVIEW);
        em.persist(photo);

        reviewsDao.updatePhotoId(review.getReviewId(), photo.getPhotoId());
        em.flush();
        em.clear();

        Review found = em.find(Review.class, review.getReviewId());
        assertEquals(photo.getPhotoId(), found.getPhotoId());
    }

    /* ---------------------------------------------------------------------- */
    /*                                DELETE                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDelete() {
        boolean deleted = reviewsDao.delete(review.getReviewId());
        em.flush();

        assertTrue(deleted);
        assertEquals(0L, countReviews());
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        boolean deleted = reviewsDao.delete(UUID.randomUUID());
        assertFalse(deleted);
        assertEquals(1L, countReviews());
    }

    /* ---------------------------------------------------------------------- */
    /*                               FIND BY ID                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFindById() {
        Optional<Review> found = reviewsDao.findById(review.getReviewId());

        assertTrue(found.isPresent());
        assertEquals(review.getReviewId(), found.get().getReviewId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Review> found = reviewsDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    @Test
    public void testSearchLinkTargetsUsesSingleStatementForLinkCollection() {
        persistMoreReviews(14);
        em.flush();
        em.clear();
        resetHibernateStats();

        Map<UUID, UUID> targets = reviewsDao.searchLinkTargets(user.getId(), null, "created_desc", 10, 0);

        assertEquals(10, targets.size());
        assertEquals(1L, statistics.getPrepareStatementCount());
    }


    /* ---------------------------------------------------------------------- */
    /*                            HELPERS                                      */
    /* ---------------------------------------------------------------------- */

    private long countReviews() {
        return em.createQuery("SELECT COUNT(r) FROM Review r", Long.class)
                .getSingleResult();
    }

    private User persistUser(String username) {
        User u = new User();
        u.setUserId(UUID.randomUUID());
        u.setUserName(username);
        u.setName("n");
        u.setSurname("l");
        u.setEmail(username + "@mail.com");
        u.setRole(Role.USER);
        u.setCreatedAt(OffsetDateTime.now());
        u.setLocale(null);
        u.setPasswordHash("password");
        u.setCellphone("123");
        em.persist(u);
        return u;
    }

    private Club persistClub(User contact) {
        Club c = new Club(
                "Club Test",
                "Location",
                new BigDecimal("5.0"),
                1,
                null,
                "desc",
                contact,
                null,
                OffsetDateTime.now(),
                null,
                Boolean.FALSE,
                null
        );
        em.persist(c);
        return c;
    }

    private Review persistReview(User u, Club c) {
        Review r = new Review();
        r.setUser(u);
        r.setClub(c);
        r.setRating(4);
        r.setDescription("Very nice");
        em.persist(r);
        return r;
    }

    private void persistMoreReviews(int amount) {
        for (int i = 0; i < amount; i++) {
            persistReview(user, persistClub(user));
        }
    }

    private void resetHibernateStats() {
        statistics.clear();
    }


}
