package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
@Transactional
public class ReviewsJpaDaoTest {

    private static final int RATING = 5;
    private static final String DESCRIPTION = "Excelente club";

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private ReviewsJpaDao reviewsDao;
    @Autowired
    private UserJpaDao userDao;
    @Autowired
    private ClubJpaDao clubDao;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Review").executeUpdate();
        em.createQuery("DELETE FROM User").executeUpdate();
        em.createQuery("DELETE FROM Club").executeUpdate();
    }

    @Test
    public void testCreateReview() {
        User user = userDao.create("user", "name","pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", Price.MEDIO, user, MusicalGenre.ROCK, Zone.ZONA_CABA, false, null);
        Review review = reviewsDao.create(user, club, RATING, DESCRIPTION);

        assertNotNull(review);
        assertEquals(Optional.of(RATING), Optional.of(review.getRating()));
        assertEquals(DESCRIPTION, review.getDescription());
        assertEquals(user.getId(), review.getUser().getId());
        assertEquals(club.getClubId(), review.getClub().getClubId());
        assertEquals(1, ((Number)em.createQuery("SELECT COUNT(r) FROM Review r").getSingleResult()).intValue());
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<Review> result = reviewsDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAllReturnsEmptyList() {
        List<Review> result = reviewsDao.findAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByUserReturnsEmptyList() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        List<Review> result = reviewsDao.findByUser(user);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByClubReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        List<Review> result = reviewsDao.findByClub(club);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        Review result = reviewsDao.update(UUID.randomUUID(), 4, "Actualizado");
        assertNull(result);
    }

    @Test
    public void testDeleteReturnsFalseIfNotFound() {
        boolean deleted = reviewsDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }
}
