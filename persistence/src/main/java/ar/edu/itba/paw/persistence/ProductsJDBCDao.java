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
        p.setProductType(ProductType.valueOf(rs.getString("product_type"))); // 'alcoholicas'|'noAlcoholicas'
        p.setIsAvailable(rs.getBoolean("is_available"));
        p.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        return p;
    };



    @Autowired
    public ProductsJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("products")
                .usingColumns("product_id","club_id","product_name","description","base_price","product_type","is_available","created_at");
    }

    @Override
    public Product create(Club club, String name, String description, BigDecimal basePrice, ProductType type, boolean isAvailable) {
        UUID id = UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("product_id", id);
        params.put("club_id", club.getClubId());
        params.put("product_name", name);
        params.put("description", description);
        params.put("base_price", basePrice);
        params.put("product_type", type.name());
        params.put("is_available", isAvailable);
        params.put("created_at", OffsetDateTime.now());
        insert.execute(params);
        return findById(id);
    }

    @Override
    public Product update(Product product) {
        final String sql = "UPDATE products SET club_id=?, product_name=?, description=?, base_price=?, product_type=?, is_available=? WHERE product_id=?";
        int upd = jdbc.update(sql,
                product.getClub().getClubId(),
                product.getProductName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getProductType().name(),
                Boolean.TRUE.equals(product.getIsAvailable()),
                product.getProductId());
        return upd > 0 ? findById(product.getProductId()) : null;
    }

    @Override
    public boolean delete(UUID id) {
        return jdbc.update("DELETE FROM products WHERE product_id = ?", id) > 0;
    }

    @Override
    public Product findById(UUID id) {
        final String sql = "SELECT product_id,club_id,product_name,description,base_price,product_type,is_available,created_at FROM products WHERE product_id = ?";
        return jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
    }

    @Override
    public Product findByName(String name) {
        final String sql = "SELECT product_id,club_id,product_name,description,base_price,product_type,is_available,created_at FROM products WHERE LOWER(product_name)=LOWER(?)";
        return jdbc.query(sql, ROW_MAPPER, name).stream().findFirst().orElse(null);
    }

    @Override
    public List<Product> findAll() {
        final String sql = "SELECT product_id,club_id,product_name,description,base_price,product_type,is_available,created_at FROM products ORDER BY product_name ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Product> findByClub(Club club) {
        final String sql = "SELECT product_id,club_id,product_name,description,base_price,product_type,is_available,created_at FROM products WHERE club_id = ?";
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }

}
