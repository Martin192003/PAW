package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Photo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PhotoJDBCDao implements PhotoDao {

    private final JdbcTemplate jdbc;

    public PhotoJDBCDao(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    private static final RowMapper<Photo> METADATA_MAPPER = (rs, i) -> {
        Photo p = new Photo();
        p.setPhotoId(rs.getObject("photo_id", UUID.class));
        p.setClubId(rs.getObject("club_id", UUID.class));
        p.setUrl(rs.getString("url"));
        p.setPostedBy(rs.getObject("posted_by", UUID.class));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) p.setCreatedAt(ts.toInstant().atOffset(ZoneOffset.UTC));
        p.setContentType(rs.getString("content_type"));
        p.setFilename(rs.getString("filename"));
        long size = rs.getLong("size_bytes");
        p.setSizeBytes(rs.wasNull() ? null : size);
        return p;
    };

    private static final RowMapper<Photo> WITH_DATA_MAPPER = (rs, i) -> {
        Photo p = METADATA_MAPPER.mapRow(rs, i);
        assert p != null;
        p.setData(rs.getBytes("data"));
        return p;
    };

    @Override
    public UUID create(UUID clubId,
                       UUID postedBy,
                       String filename,
                       String contentType,
                       byte[] data) {
        UUID id = UUID.randomUUID();
        final String insertSql = """
            INSERT INTO photos (photo_id, club_id, url, posted_by, created_at, data, content_type, filename, size_bytes)
            VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?)
            """;
        jdbc.update(
                insertSql,
                id,
                clubId,
                "/photos/pending",
                postedBy,
                data,
                contentType,
                filename,
                (long) data.length
        );
        jdbc.update("UPDATE photos SET url = ? WHERE photo_id = ?",
                "/photos/" + id, id);

        return id;
    }

    @Override
    public List<Photo> findByClub(UUID clubId, int limit, int offset) {
        final String sql = """
            SELECT *
            FROM photos
            WHERE club_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, METADATA_MAPPER, clubId, limit, offset);
    }

    @Override
    public Optional<Photo> findByIdWithData(UUID photoId) {
        final String sql = """
            SELECT *
            FROM photos
            WHERE photo_id = ?
            """;
        List<Photo> list = jdbc.query(sql, WITH_DATA_MAPPER, photoId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Photo> findById(UUID photoId) {
        final String sql = """
            SELECT *
            FROM photos
            WHERE photo_id = ?
            """;
        List<Photo> list = jdbc.query(sql, METADATA_MAPPER, photoId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public UUID save(Photo p) {
        final String insertSql = """
      INSERT INTO photos (club_id, posted_by, filename, content_type, size_bytes, data, url, created_at)
      VALUES (?, ?, ?, ?, ?, ?, '/photos/pending', NOW())
      RETURNING photo_id
    """;

        UUID id = jdbc.queryForObject(insertSql,
                (rs, i) -> rs.getObject("photo_id", UUID.class),
                p.getClubId(),
                p.getPostedBy(),
                p.getFilename(),
                p.getContentType(),
                p.getSizeBytes(),
                p.getData()
        );

        jdbc.update("UPDATE photos SET url = ? WHERE photo_id = ?", "/photos/" + id, id);
        return id;
    }

    @Override
    public Optional<Photo> findByBooking(UUID bookingId){
        final String sql = """
            SELECT p.*
            FROM photos p
            JOIN bookings b ON p.posted_by = b.user_id
            WHERE b.booking_id = ?
            """;
        return jdbc.query(sql, METADATA_MAPPER, bookingId).stream().findFirst();
    }

    @Override
    public boolean delete(UUID photoId) {
        int updated = jdbc.update("DELETE FROM photos WHERE photo_id = ?", photoId);
        return updated > 0;
    }
}