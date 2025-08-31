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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                .usingColumns("photo_id", "club_id", "url", "posted_by", "created_at");
        this.clubDao = clubDao;
        this.userDao = userDao;
    }

    @Override
    public Photo create(String url, Club club, User postedBy) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put( "photo_id", id);
        params.put("club_id", club.getClubId());
        params.put("url", url);
        params.put("posted_by", postedBy.getId());
        params.put("created_at", OffsetDateTime.now());

        insert.execute(params);
        return findById(id);
    }

    @Override
    public Photo update(Photo photo) {
        final String sql = "UPDATE photos SET url = ? WHERE photo_id = ?";
        int updated = jdbc.update(sql, photo.getUrl(), photo.getPhotoId());
        return updated > 0 ? findById(photo.getPhotoId()) : null;
    }

    @Override
    public boolean delete(UUID id) {
        final String sql = "DELETE FROM photos WHERE photo_id = ?";
        return jdbc.update(sql, id) > 0;
    }

    @Override
    public Photo findById(UUID id) {
        final String sql = "SELECT photo_id, club_id, url, posted_by, created_at FROM photos WHERE photo_id = ?";
        Photo p = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        if (p != null) {
            p.setClub(clubDao.findById(p.getClub().getClubId()));
            p.setPostedBy(userDao.findById(p.getPostedBy().getId()));
        }
        return p;
    }

    @Override
    public List<Photo> findByClub(Club club) {
        final String sql = "SELECT photo_id, club_id, url, posted_by, created_at FROM photos WHERE club_id = ?";
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }

    @Override
    public List<Photo> findByUser(User user) {
        final String sql = "SELECT photo_id, club_id, url, posted_by, created_at FROM photos WHERE posted_by = ?";
        return jdbc.query(sql, ROW_MAPPER, user.getId());
    }

}
