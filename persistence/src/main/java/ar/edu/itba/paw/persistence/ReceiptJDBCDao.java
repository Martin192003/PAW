package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Receipt;
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
public class ReceiptJDBCDao implements ReceiptDao {

    private final JdbcTemplate jdbc;

    public ReceiptJDBCDao(DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
    }

    private static final RowMapper<Receipt> META = (rs, i) -> {
        Receipt r = new Receipt();
        r.setReceiptId(rs.getObject("receipt_id", UUID.class));
        r.setBookingId(rs.getObject("booking_id", UUID.class));
        r.setUploadedBy(rs.getObject("uploaded_by", UUID.class));
        r.setUrl(rs.getString("url"));
        r.setFilename(rs.getString("filename"));
        r.setContentType(rs.getString("content_type"));
        long size = rs.getLong("size_bytes");
        r.setSizeBytes(rs.wasNull() ? null : size);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toInstant().atOffset(ZoneOffset.UTC));
        return r;
    };

    private static final RowMapper<Receipt> WITH_DATA = (rs, i) -> {
        Receipt r = META.mapRow(rs, i);
        assert r != null;
        r.setData(rs.getBytes("data"));
        return r;
    };

    @Override
    public UUID create(UUID bookingId, UUID uploadedBy, String filename, String contentType, byte[] data) {
        UUID id = UUID.randomUUID();
        final String insertSql = """
            INSERT INTO receipts (receipt_id, booking_id, uploaded_by, url, created_at, data, content_type, filename, size_bytes)
            VALUES (?, ?, ?, '/receipts/pending', NOW(), ?, ?, ?, ?)
            """;
        jdbc.update(
            insertSql,
            id,
            bookingId,
            uploadedBy,
            data,
            contentType,
            filename,
            (long) data.length
        );
        jdbc.update("UPDATE receipts SET url = ? WHERE receipt_id = ?", "/receipts/" + id, id);
        return id;
    }

    @Override
    public Optional<Receipt> findByBooking(UUID bookingId) {
        final String sql = "SELECT * FROM receipts WHERE booking_id = ?";
        List<Receipt> list = jdbc.query(sql, META, bookingId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Receipt> findById(UUID receiptId) {
        final String sql = "SELECT * FROM receipts WHERE receipt_id = ?";
        List<Receipt> list = jdbc.query(sql, META, receiptId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Receipt> findByIdWithData(UUID receiptId) {
        final String sql = "SELECT * FROM receipts WHERE receipt_id = ?";
        List<Receipt> list = jdbc.query(sql, WITH_DATA, receiptId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public boolean deleteByBooking(UUID bookingId) {
        return jdbc.update("DELETE FROM receipts WHERE booking_id = ?", bookingId) > 0;
    }

    @Override
    public boolean delete(UUID receiptId) {
        return jdbc.update("DELETE FROM receipts WHERE receipt_id = ?", receiptId) > 0;
    }
}