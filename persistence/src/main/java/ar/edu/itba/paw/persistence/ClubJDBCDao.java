package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
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
        c.setRating((BigDecimal) rs.getObject("rating")); // puede ser null
        c.setQuantityRatings(rs.getObject("quantity_ratings") == null ? null : rs.getInt("quantity_ratings"));
        c.setPriceRange(rs.getString("price_range") == null ? null : Price.valueOf(rs.getString("price_range").toUpperCase()));
        c.setDescription(rs.getString("description"));
        c.setZone(rs.getString("zone") == null ? null : Zone.valueOf(rs.getString("zone")));

        UUID contactId = rs.getObject("contact_info", UUID.class);
        if (contactId != null) {
            User u = new User();
            u.setUserId(contactId);
            c.setContactInfo(u);
        } else {
            c.setContactInfo(null);
        }

        c.setMusicGenre(rs.getString("music_genre") == null ? null : MusicalGenre.valueOf(rs.getString("music_genre").toUpperCase()));
        c.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return c;
    };

    @Autowired
    public ClubJDBCDao(final DataSource ds, final UserDao userDao) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("clubs")
                .usingColumns("club_id","club_name","location","rating","quantity_ratings","price_range","description","contact_info","music_genre","zone");
        this.userDao = userDao;
    }

    @Override
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("club_id", id);
        params.put("club_name", name);
        params.put("location", location);
        params.put("rating", null);
        params.put("quantity_ratings", 0);
        params.put("price_range", priceRange != null ? priceRange.name().toLowerCase() : null);
        params.put("description", description);
        params.put("contact_info", contactInfo != null ? contactInfo.getId() : null);
        params.put("music_genre", musicGenre != null ? musicGenre.name().toLowerCase() : null);
        params.put("zone", zone != null ? zone.name() : null);

        insert.execute(params);

        if (contactInfo != null && contactInfo.getId() != null) {
            jdbc.update("UPDATE users SET role = 'admin' WHERE user_id = ?", contactInfo.getId());
        }

        return findById(id).orElseThrow();
    }

    @Override
    public Club update(UUID clubId,
                       String name,
                       String location,
                       BigDecimal rating,
                       Integer quantityRatings,
                       Price priceRange,
                       String description,
                       UUID contactInfoId,
                       MusicalGenre musicGenre,
                       Zone zone) {

        Objects.requireNonNull(clubId, "clubId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(location, "location is required");
        Objects.requireNonNull(priceRange, "priceRange is required");

        // validate priceRange using its lowercase name
        if (!Set.of("bajo","medio","alto").contains(priceRange.name().toLowerCase())) {
            throw new IllegalArgumentException("priceRange debe ser bajo|medio|alto");
        }
        if (rating != null) {
            if (rating.compareTo(new BigDecimal("1.0")) < 0 || rating.compareTo(new BigDecimal("5.0")) > 0) {
                throw new IllegalArgumentException("rating fuera de rango (1..5): " + rating);
            }
        }
        if (quantityRatings != null && quantityRatings < 0) {
            throw new IllegalArgumentException("quantity_ratings debe ser >= 0");
        }

        final String sql = "UPDATE clubs " +
                "SET club_name = ?, location = ?, rating = ?, quantity_ratings = ?, price_range = ?, " +
                "    description = ?, contact_info = ?, music_genre = ?, zone = ? " +
                "WHERE club_id = ?";

        int upd = jdbc.update(sql,
                name,
                location,
                rating,
                quantityRatings,
                priceRange != null ? priceRange.name().toLowerCase() : null,
                description,
                contactInfoId,
                musicGenre != null ? musicGenre.name().toLowerCase() : null,
                zone != null ? zone.name() : null,
                clubId
        );

        return (upd > 0) ? findById(clubId).orElse(null) : null;
    }




    @Override
    public boolean delete(UUID clubId) {
        return jdbc.update("DELETE FROM clubs WHERE club_id = ?", clubId) > 0;
    }

    @Override
    public Integer incrementQuantityRatings(UUID clubId, int delta) {
        Objects.requireNonNull(clubId, "clubId is required");

        final String sql =
                "UPDATE clubs " +
                        "SET quantity_ratings = GREATEST(0, COALESCE(quantity_ratings, 0) + ?) " +
                        "WHERE club_id = ? " +
                        "RETURNING quantity_ratings";

        try {
            return jdbc.queryForObject(sql, Integer.class, delta, clubId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null; // club no existe
        }
    }

    @Override
    public Optional<Club> findById(UUID clubId) {
        final String sql = "SELECT * FROM clubs WHERE club_id = ?";
        Club c = jdbc.query(sql, ROW_MAPPER, clubId).stream().findFirst().orElse(null);
        if (c != null) c.setContactInfo(userDao.findById(c.getContactInfo().getId()).orElse(null));
        return Optional.ofNullable(c);
    }

    @Override
    public Optional<Club> findByName(String clubName) {
        final String sql = "SELECT * FROM clubs WHERE LOWER(club_name)=LOWER(?)";
        Club c = jdbc.query(sql, ROW_MAPPER, clubName).stream().findFirst().orElse(null);
        if (c != null) c.setContactInfo(userDao.findById(c.getContactInfo().getId()).orElse(null));
        return Optional.ofNullable(c);
    }

    @Override
    public List<Club> findAll() {
        final String sql = "SELECT * FROM clubs ORDER BY club_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }


    @Override
    public List<Club> search(String nameLike,
                             Zone location,
                             String musicGenre,
                             String priceRange,
                             BigDecimal minRating,
                             BigDecimal maxRating) {

        StringBuilder sql = new StringBuilder("SELECT * FROM clubs WHERE 1=1");
        List<Object> args = new ArrayList<>();

        if (nameLike != null && !nameLike.isBlank()) {
            sql.append(" AND LOWER(club_name) ILIKE ? ESCAPE '\\'");
            args.add(likeContains(nameLike));
        }
        if (location != null ) {
            sql.append(" AND LOWER(zone) = LOWER(?)");
            args.add(location.name());
        }
        if (musicGenre != null && !musicGenre.isBlank()) {
            sql.append(" AND LOWER(music_genre) = LOWER(?)");
            args.add(musicGenre.trim());
        }
        if (priceRange != null && !priceRange.isBlank()) {
            if (!Set.of("bajo","medio","alto").contains(priceRange)) {
                throw new IllegalArgumentException("priceRange inválido");
            }
            sql.append(" AND price_range = ?");
            args.add(priceRange);
        }
        if (minRating != null) {
            sql.append(" AND rating >= ?");
            args.add(minRating);
        }
        if (maxRating != null) {
            sql.append(" AND rating <= ?");
            args.add(maxRating);
        }
        sql.append(" ORDER BY club_name ASC");

        return jdbc.query(sql.toString(), ROW_MAPPER, args.toArray());
    }


    public static String escapeLike(String param) {
        if (param == null) return null;
        return param
                .replace("\\", "\\\\")  // primero escapás la barra invertida
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private static String likeContains(String s) {
        return "%" + escapeLike(s.trim()) + "%";
    }

}
