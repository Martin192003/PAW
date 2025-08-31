package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class ClubJDBCDao implements ClubDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;
    private final UserDao userDao;

    private static final RowMapper<Club> ROW_MAPPER = (rs, i) -> {
        Club c = new Club();
        c.setClubId(rs.getObject("club_id", UUID.class));
        c.setClubName(rs.getString("club_name"));
        c.setLocation(rs.getString("location"));
        c.setRating(rs.getBigDecimal("rating"));
        c.setQuantityRatings(rs.getObject("quantity_ratings") == null ? null : rs.getInt("quantity_ratings"));
        c.setPriceRange(rs.getString("price_range"));
        c.setDescription(rs.getString("description"));
        User u = new User(); u.setUserId(rs.getObject("contact_info", UUID.class)); c.setContactInfo(u);
        c.setMusicGenre(rs.getString("music_genre"));
        c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return c;
    };

    @Autowired
    public ClubJDBCDao(final DataSource ds, final UserDao userDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("clubs")
                .usingColumns("club_id","club_name","location","rating","quantity_ratings","price_range","description","contact_info","music_genre","created_at");
        this.userDao = userDao;
    }

    @Override
    public Club create(String name, String description, String location, String priceRange, User contactInfo, String musicGenre) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("club_id", id);
        params.put("club_name", name);
        params.put("location", location);
        params.put("rating", null);
        params.put("quantity_ratings", 0);
        params.put("price_range", priceRange);
        params.put("description", description);
        params.put("contact_info", contactInfo.getId());
        params.put("music_genre", musicGenre);
        params.put("created_at", OffsetDateTime.now());
        insert.execute(params);
        return findById(id);
    }

    @Override
    public Club update(Club club) {
        final String sql = "UPDATE clubs SET club_name=?, location=?, rating=?, quantity_ratings=?, price_range=?, description=?, contact_info=?, music_genre=? WHERE club_id=?";
        // Validación simple (DB ya valida 1..5):
        if (club.getRating() != null) {
            BigDecimal r = club.getRating();
            if (r.compareTo(new BigDecimal("1.0")) < 0 || r.compareTo(new BigDecimal("5.0")) > 0)
                throw new IllegalArgumentException("rating fuera de rango (1..5): " + r);
        }
        int upd = jdbc.update(sql,
                club.getClubName(),
                club.getLocation(),
                club.getRating(),
                club.getQuantityRatings(),
                club.getPriceRange(),
                club.getDescription(),
                club.getContactInfo().getId(),
                club.getMusicGenre(),
                club.getClubId());
        return upd > 0 ? findById(club.getClubId()) : null;
    }

    @Override
    public boolean delete(UUID clubId) {
        return jdbc.update("DELETE FROM clubs WHERE club_id = ?", clubId) > 0;
    }

    @Override
    public Club findById(UUID clubId) {
        final String sql = "SELECT club_id,club_name,location,rating,quantity_ratings,price_range,description,contact_info,music_genre,created_at FROM clubs WHERE club_id = ?";
        Club c = jdbc.query(sql, ROW_MAPPER, clubId).stream().findFirst().orElse(null);
        if (c != null) c.setContactInfo(userDao.findById(c.getContactInfo().getId()));
        return c;
    }

    @Override
    public Club findByName(String clubName) {
        final String sql = "SELECT club_id,club_name,location,rating,quantity_ratings,price_range,description,contact_info,music_genre,created_at FROM clubs WHERE LOWER(club_name)=LOWER(?)";
        Club c = jdbc.query(sql, ROW_MAPPER, clubName).stream().findFirst().orElse(null);
        if (c != null) c.setContactInfo(userDao.findById(c.getContactInfo().getId()));
        return c;
    }

    @Override
    public List<Club> findAll() {
        final String sql = "SELECT club_id,club_name,location,rating,quantity_ratings,price_range,description,contact_info,music_genre,created_at FROM clubs ORDER BY club_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

}
