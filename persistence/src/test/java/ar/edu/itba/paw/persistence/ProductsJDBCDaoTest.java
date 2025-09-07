package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.jdbc.JdbcTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Sql("classpath:schema.sql")
public class ProductsJDBCDaoTest {

    private static final String NAME = "Producto";
    private static final String DESCRIPTION = "Descripción";
    private static final BigDecimal BASE_PRICE = BigDecimal.valueOf(100);
    private static final ProductType TYPE = ProductType.ALCOHOLICA;
    private static final boolean IS_AVAILABLE = true;

    @Autowired
    private DataSource ds;
    @Autowired
    private ProductsJDBCDao productsDao;
    @Autowired
    private ClubDao clubDao;
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserDao userDao;

    @Before
    public void setUp() {
        jdbcTemplate = new JdbcTemplate(ds);
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "products");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "clubs");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "users");
    }

    @Test
    public void testCreateProduct() {
        // Crear usuario de contacto
        User contact = userDao.create("admin", "pass", "last", "email@email.com", "123456", Role.USER);

        // Crear club con usuario de contacto
        Club club = clubDao.create("Club", "Desc", "Loc", "medio", contact, "Rock", Zone.ZONA_CABA);

        Product product = productsDao.create(club, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);

        assertNotNull(product);
        assertEquals(NAME, product.getProductName());
        assertEquals(DESCRIPTION, product.getDescription());
        assertEquals(BASE_PRICE, BigDecimal.valueOf(product.getBasePrice().intValue()));        assertEquals(TYPE, product.getProductType());
        assertEquals(IS_AVAILABLE, product.getIsAvailable());
        assertEquals(club.getClubId(), product.getClub().getClubId());
        assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "products"));
    }

    @Test
    public void testFindByIdReturnsEmpty() {
        Optional<Product> result = productsDao.findById(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByNameReturnsEmpty() {
        Optional<Product> result = productsDao.findByName("NoExiste");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindAllReturnsEmptyList() {
        List<Product> result = productsDao.findAll();
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateReturnsNullIfNotFound() {
        Product result = productsDao.update(UUID.randomUUID(), UUID.randomUUID(), "Nuevo", "Nueva desc", BigDecimal.valueOf(200), ProductType.NO_ALCOHOLICA, false);
        assertNull(result);
    }

    @Test
    public void testDeleteReturnsFalseIfNotFound() {
        boolean deleted = productsDao.delete(UUID.randomUUID());
        assertFalse(deleted);
    }

    @Test
    public void testFindByClubReturnsEmptyList() {
        Club club = new Club();
        club.setClubId(UUID.randomUUID());
        List<Product> result = productsDao.findByClub(club);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFindByClubIDReturnsEmptyList() {
        List<Product> result = productsDao.findByClubID(UUID.randomUUID());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}