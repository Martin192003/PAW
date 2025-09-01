package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
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
public class PhotoJDBCDao implements PhotoDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final ClubDao clubDao;
    private final UserDao userDao;

    private static final RowMapper<Photo> ROW_MAPPER = (rs, rowNum) -> {
        Photo p = new Photo();
        p.setPhotoId(rs.getObject("photo_id", UUID.class));
        Club c = new Club();
        c.setClubId(rs.getObject("club_id", UUID.class));
        p.setClub(c);
        p.setUrl(rs.getString("url"));
        User u = new User();
        u.setUserId(rs.getObject("posted_by", UUID.class));
        p.setPostedBy(u);
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return p;
    };

    @Autowired
    public PhotoJDBCDao(final DataSource ds, final ClubDao clubDao, final UserDao userDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("photos")
                .usingColumns("photo_id", "club_id", "url", "posted_by");
        this.clubDao = clubDao;
        this.userDao = userDao;
    }

    @Override
    public Photo create(String url, Club club, User postedBy) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("photo_id", id);
        params.put("club_id", club.getClubId());
        params.put("url", url);
        params.put("posted_by", postedBy.getId());

        insert.execute(params);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted photo not found: " + id));
    }

    @Override
    public Photo update(UUID photoId, String url) {
        Objects.requireNonNull(photoId, "photoId is required");
        Objects.requireNonNull(url, "url is required");

        final String sql = "UPDATE photos SET url = ? WHERE photo_id = ?";
        int updated = jdbc.update(sql, url, photoId);

        return updated > 0 ? findById(photoId).orElse(null) : null;
    }

    @Override
    public boolean delete(UUID id) {
        final String sql = "DELETE FROM photos WHERE photo_id = ?";
        return jdbc.update(sql, id) > 0;
    }

    @Override
    public Optional<Photo> findById(UUID id) {
        final String sql = "SELECT * FROM photos WHERE photo_id = ?";
        Photo p = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        if (p != null) {
            p.setClub(clubDao.findById(p.getClub().getClubId()).orElse(null));
            p.setPostedBy(userDao.findById(p.getPostedBy().getId()).orElse(null));
        }
        return Optional.ofNullable(p);
    }

    @Override
    public List<Photo> findByClub(Club club) {
        final String sql = "SELECT * FROM photos WHERE club_id = ?";
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }

    @Override
    public List<Photo> findByUser(User user) {
        final String sql = "SELECT * FROM photos WHERE posted_by = ?";
        return jdbc.query(sql, ROW_MAPPER, user.getId());
    }

}
