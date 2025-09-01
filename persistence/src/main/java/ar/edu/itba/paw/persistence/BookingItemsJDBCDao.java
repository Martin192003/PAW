package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Booking;
import ar.edu.itba.paw.model.BookingItem;
import ar.edu.itba.paw.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class BookingItemsJDBCDao implements BookingItemsDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<BookingItem> ROW_MAPPER = (rs, rowNum) -> {
        BookingItem bi = new BookingItem();
        bi.setBookingItemId(rs.getObject("booking_item_id", UUID.class));

        Booking booking = new Booking();
        booking.setBookingId(rs.getObject("booking_id", UUID.class));
        bi.setBooking(booking);

        Product product = new Product();
        product.setProductId(rs.getObject("product_id", UUID.class));
        bi.setProduct(product);

        bi.setQuantity(rs.getInt("quantity"));
        bi.setUnitPrice(rs.getBigDecimal("unit_price"));
        return bi;
    };

    @Autowired
    public BookingItemsJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("booking_items")
                .usingColumns("booking_item_id", "booking_id", "product_id", "quantity", "unit_price", "line_total");
    }

    @Override
    public BookingItem create(Booking booking, Product product, int quantity, BigDecimal unitPrice) {
        UUID id = UUID.randomUUID();
        HashMap<String,Object> params = new HashMap<>();

        params.put("booking_item_id", id);
        params.put("booking_id", booking.getBookingId());
        params.put("product_id", product.getProductId());
        params.put("quantity", quantity);
        params.put("unit_price", unitPrice);
        insert.execute(params);
        return new BookingItem(id, booking, product, quantity, unitPrice, lineTotal(id));
    }

    @Override
    public BookingItem update(UUID id, Booking booking, Product product, int quantity, BigDecimal unitPrice) {
        final String sql = "UPDATE booking_items SET booking_id=?, product_id=?, quantity=?, unit_price=?, line_total=? WHERE booking_item_id=?";
        int updated = jdbc.update(sql,
                booking.getBookingId(),
                product.getProductId(),
                quantity,
                unitPrice,
                id
        );
        return updated > 0 ? new BookingItem(id, booking, product, quantity, unitPrice, lineTotal(id)) : null;
    }

    @Override
    public boolean delete(UUID id) {
        final String sql = "DELETE FROM booking_items WHERE booking_item_id = ?";
        return jdbc.update(sql, id) > 0;
    }

    @Override
    public Optional<BookingItem> findById(UUID id) {
        final String sql = "SELECT booking_item_id, booking_id, product_id, quantity, unit_price, line_total FROM booking_items WHERE booking_item_id = ?";
        BookingItem bi = jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
        return Optional.ofNullable(bi);
    }

    @Override
    public List<BookingItem> findAll() {
        final String sql = "SELECT booking_item_id, booking_id, product_id, quantity, unit_price, line_total FROM booking_items";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<BookingItem> findByBooking(Booking booking) {
        final String sql = "SELECT booking_item_id, booking_id, product_id, quantity, unit_price, line_total FROM booking_items WHERE booking_id = ?";
        return jdbc.query(sql, ROW_MAPPER, booking.getBookingId());
    }

    @Override
    public List<BookingItem> findByProduct(Product product) {
        final String sql = "SELECT booking_item_id, booking_id, product_id, quantity, unit_price, line_total FROM booking_items WHERE product_id = ?";
        return jdbc.query(sql, ROW_MAPPER, product.getProductId());
    }

    private BigDecimal lineTotal(UUID id) {
        return jdbc.queryForObject("SELECT line_total FROM booking_items WHERE booking_item_id = ?", BigDecimal.class, id);
    }

}
