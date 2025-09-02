// persistence/src/test/java/ar/edu/itba/paw/persistence/ReviewsJDBCDaoTest.java
package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ReviewsJDBCDaoTest {

    private static final int RATING = 5;
    private static final String DESCRIPTION = "Excelente club";

    @Autowired
    private DataSource ds;
    @Autowired
    private ReviewsJDBCDao reviewsDao;
    @Autowired
    private UserJDBCDao userDao;
    @Autowired
    private ClubJDBCDao clubDao;
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "reviews");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
    }

    @Test
    public void testCreateReview() {
        User user = userDao.create("user", "pass", "last", "email", "cell", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", "medio", user, "Rock");
        Review review = reviewsDao.create(user, club, RATING, DESCRIPTION);

        assertNotNull(review);
        assertEquals(Optional.of(RATING), Optional.of(review.getRating()));
        assertEquals(DESCRIPTION, review.getDescription());
        assertEquals(user.getId(), review.getUser().getId());
        assertEquals(club.getClubId(), review.getClub().getClubId());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "reviews"));
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
