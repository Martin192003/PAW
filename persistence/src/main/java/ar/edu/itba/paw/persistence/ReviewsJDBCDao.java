package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Review;
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
public class ReviewsJDBCDao implements ReviewsDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final UserDao userDao;
    private final ClubDao clubDao;

    private static final RowMapper<Review> ROW_MAPPER = (rs, i) -> {
        Review r = new Review();
        r.setReviewId(rs.getObject("review_id", UUID.class));
        User u = new User(); u.setUserId(rs.getObject("user_id", UUID.class)); r.setUser(u);
        Club c = new Club(); c.setClubId(rs.getObject("club_id", UUID.class)); r.setClub(c);
        r.setRating(rs.getInt("rating"));
        r.setDescription(rs.getString("description"));
        r.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return r;
    };

    @Autowired
    public ReviewsJDBCDao(final DataSource ds, final UserDao userDao, final ClubDao clubDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("reviews")
                .usingColumns("review_id","user_id","club_id","rating","description","created_at");
        this.userDao = userDao;
        this.clubDao = clubDao;
    }

    @Override
    public Review create(User user, Club club, int rating, String description) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("review_id", id);
        params.put("user_id", user.getId());
        params.put("club_id", club.getClubId());
        params.put("rating", rating);
        params.put("description", description);
        params.put("created_at", OffsetDateTime.now());
        insert.execute(params);
        return findById(id);
    }

    @Override
    public Review update(Review review) {
        final String sql = "UPDATE reviews SET rating=?, description=? WHERE review_id=?";
        int upd = jdbc.update(sql, review.getRating(), review.getDescription(), review.getReviewId());
        return upd > 0 ? findById(review.getReviewId()) : null;
    }

    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM reviews WHERE review_id = ?", id) > 0;
    }

    @Override
    public Review findById(UUID id) {
        final String sql = "SELECT review_id,user_id,club_id,rating,description,created_at FROM reviews WHERE review_id = ?";
        Review r = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        if (r != null) {
            r.setUser(userDao.findById(r.getUser().getId()));
            r.setClub(clubDao.findById(r.getClub().getClubId()));
        }
        return r;
    }

    @Override
    public List<Review> findAll() {
        final String sql = "SELECT review_id,user_id,club_id,rating,description,created_at FROM reviews ORDER BY created_at DESC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Review> findByUser(User user) {
        final String sql = "SELECT review_id,user_id,club_id,rating,description,created_at FROM reviews WHERE user_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, ROW_MAPPER, user.getId());
    }

    @Override
    public List<Review> findByClub(Club club) {
        final String sql = "SELECT review_id,user_id,club_id,rating,description,created_at FROM reviews WHERE club_id = ? ORDER BY created_at DESC";
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }


}
