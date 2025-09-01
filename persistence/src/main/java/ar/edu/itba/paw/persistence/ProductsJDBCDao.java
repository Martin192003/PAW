package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
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
public class ProductsJDBCDao implements ProductsDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<Product> ROW_MAPPER = (rs, i) -> {
        Product p = new Product();
        p.setProductId(rs.getObject("product_id", UUID.class));

        Club c = new Club();
        c.setClubId(rs.getObject("club_id", UUID.class));
        p.setClub(c);

        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setBasePrice(rs.getBigDecimal("base_price"));
        p.setProductType(ProductType.fromDb(rs.getString("product_type")));
        p.setIsAvailable((Boolean) rs.getObject("is_available")); // maneja NULL correctamente
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return p;
    };

    @Autowired
    public ProductsJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("products")
                // sin created_at para que lo setee la DB
                .usingColumns("product_id","club_id","product_name","description","base_price","product_type","is_available");
    }

    @Override
    public Product create(Club club, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable) {
        Objects.requireNonNull(club, "club is required");
        Objects.requireNonNull(club.getClubId(), "clubId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(basePrice, "basePrice is required");
        if (basePrice.signum() < 0) throw new IllegalArgumentException("basePrice must be >= 0");
        Objects.requireNonNull(type, "type is required");

        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("product_id", id);
        params.put("club_id", club.getClubId());
        params.put("product_name", name);
        params.put("description", description);
        params.put("base_price", basePrice);
        params.put("product_type", type.db());
        params.put("is_available", isAvailable);

        insert.execute(params);
        return findById(id).orElseThrow(() -> new IllegalStateException("Inserted product not found: " + id));
    }

    // update por parámetros (no Product)
    @Override
    public Product update(UUID productId, UUID clubId, String name, String description,
                          BigDecimal basePrice, ProductType type, Boolean isAvailable) {
        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(clubId, "clubId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(basePrice, "basePrice is required");
        if (basePrice.signum() < 0) throw new IllegalArgumentException("basePrice must be >= 0");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(isAvailable, "isAvailable is required");

        final String sql = "UPDATE products " +
                "SET club_id = ?, product_name = ?, description = ?, base_price = ?, product_type = ?, is_available = ? " +
                "WHERE product_id = ?";

        int upd = jdbc.update(sql,
                clubId,
                name,
                description,
                basePrice,
                type.db(),
                isAvailable,
                productId
        );

        return (upd > 0) ? findById(productId).orElse(null) : null;
    }

    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM products WHERE product_id = ?", id) > 0;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        final String sql = "SELECT * FROM products WHERE product_id = ?";
        return jdbc.query(sql, ROW_MAPPER, id).stream().findFirst();
    }

    @Override
    public Optional<Product> findByName(String name) {
        final String sql = "SELECT * FROM products WHERE LOWER(product_name)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, name).stream().findFirst();
    }

    @Override
    public List<Product> findAll() {
        final String sql = "SELECT * FROM products ORDER BY product_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Product> findByClub(Club club) {
        final String sql = "SELECT * FROM products WHERE club_id = ? ORDER BY product_name ASC"; // 👈 espacio + orden
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }

    @Override
    public List<Product> findByClubID(UUID clubId) {
        final String sql = "SELECT * FROM products WHERE club_id = ? ORDER BY product_name ASC";
        return jdbc.query(sql, ROW_MAPPER, clubId);
    }
}
