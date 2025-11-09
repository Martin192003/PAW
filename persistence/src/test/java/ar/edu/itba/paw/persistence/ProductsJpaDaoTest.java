/*package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ProductsJpaDaoTest {

    private static final String NAME = "Producto";
    private static final String DESCRIPTION = "Descripción";
    private static final BigDecimal BASE_PRICE = BigDecimal.valueOf(100);
    private static final ProductType TYPE = ProductType.ALCOHOLICA;
    private static final boolean IS_AVAILABLE = true;

    @Autowired private ProductsJpaDao productsDao;
    @Autowired private ClubDao clubDao;
    @Autowired private UserDao userDao;

    @PersistenceContext private EntityManager em;

    /*@Before
    public void setUp() {
        // Limpiar con JPQL para no depender de schema.sql ni JdbcTemplate
        // Orden: hijos -> padres (por FKs)
        safeDelete("DELETE FROM Booking b");
        safeDelete("DELETE FROM Product p"); // tabla products mapeada por entidad Product
        safeDelete("DELETE FROM ClubTable t");
        // si existe tabla intermedia de M:N admins
        safeDelete("DELETE FROM Club c");
        safeDelete("DELETE FROM User u");
        em.flush();
    }

    private void safeDelete(String jpql) {
        try { em.createQuery(jpql).executeUpdate(); } catch (Exception ignored) { }
    }

    @Test
    public void testCreateProduct() {
        User contact = userDao.create("admin", "name","pass", "last", "email@email.com", "123456", Role.USER);
        Club club = clubDao.create("Club", "Desc", "Loc", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);

        Product product = productsDao.create(club, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);

        assertNotNull(product);
        assertEquals(NAME, product.getProductName());
        assertEquals(DESCRIPTION, product.getDescription());
        assertEquals(0, BASE_PRICE.compareTo(product.getBasePrice()));
        assertEquals(TYPE, product.getProductType());
        assertEquals(IS_AVAILABLE, product.getIsAvailable());
        assertEquals(club.getClubId(), product.getClub().getClubId());

        long count = em.createQuery("select count(p) from Product p", Long.class).getSingleResult();
        assertEquals(1L, count);
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

    @Test
    public void testUpdateSuccess() {
        User contact = userDao.create("admin2", "name2","pass2", "last2", "email2@email.com", "654321", Role.USER);
        Club club = clubDao.create("Club2", "Desc2", "Loc2", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        Product product = productsDao.create(club, "Prod", "Desc", BigDecimal.valueOf(50), ProductType.NO_ALCOHOLICA, true);

        Product updated = productsDao.update(product.getProductId(), club.getClubId(), "ProdUp", "DescUp", BigDecimal.valueOf(60), ProductType.ALCOHOLICA, false);

        assertNotNull(updated);
        assertEquals("ProdUp", updated.getProductName());
        assertEquals("DescUp", updated.getDescription());
        assertEquals(0, BigDecimal.valueOf(60).compareTo(updated.getBasePrice()));
        assertEquals(ProductType.ALCOHOLICA, updated.getProductType());
        assertFalse(updated.getIsAvailable());
    }

    @Test
    public void testDeleteSuccess() {
        User contact = userDao.create("admin3", "name3","pass3", "last3", "email3@email.com", "987654", Role.USER);
        Club club = clubDao.create("Club3", "Desc3", "Loc3", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        Product product = productsDao.create(club, "ProdDel", "DescDel", BigDecimal.valueOf(70), ProductType.ALCOHOLICA, true);

        boolean deleted = productsDao.delete(product.getProductId());
        assertTrue(deleted);
        assertTrue(productsDao.findById(product.getProductId()).isEmpty());
    }

    @Test
    public void testFindByIdSuccess() {
        User contact = userDao.create("admin4", "name4","pass4", "last4", "email4@email.com", "111111", Role.USER);
        Club club = clubDao.create("Club4", "Desc4", "Loc4", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        Product product = productsDao.create(club, "ProdFindId", "DescFindId", BigDecimal.valueOf(80), ProductType.ALCOHOLICA, true);

        Optional<Product> found = productsDao.findById(product.getProductId());
        assertTrue(found.isPresent());
        assertEquals("ProdFindId", found.get().getProductName());
    }

    @Test
    public void testFindByNameSuccess() {
        User contact = userDao.create("admin5", "name5","pass5", "last5", "email5@email.com", "222222", Role.USER);
        Club club = clubDao.create("Club5", "Desc5", "Loc5", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        Product product = productsDao.create(club, "ProdFindName", "DescFindName", BigDecimal.valueOf(90), ProductType.ALCOHOLICA, true);

        Optional<Product> found = productsDao.findByName("ProdFindName");
        assertTrue(found.isPresent());
        assertEquals(product.getProductId(), found.get().getProductId());
    }

    @Test
    public void testFindByClubWithProducts() {
        User contact = userDao.create("admin7", "name7","pass7", "last7", "email7@email.com", "444444", Role.USER);
        Club club = clubDao.create("Club7", "Desc7", "Loc7", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        productsDao.create(club, "ProdA", "DescA", BigDecimal.valueOf(30), ProductType.ALCOHOLICA, true);
        productsDao.create(club, "ProdB", "DescB", BigDecimal.valueOf(40), ProductType.NO_ALCOHOLICA, true);

        List<Product> products = productsDao.findByClub(club);
        assertEquals(2, products.size());
    }

    @Test
    public void testFindByClubIDWithProducts() {
        User contact = userDao.create("admin8", "name8","pass8", "last8", "email8@email.com", "555555", Role.USER);
        Club club = clubDao.create("Club8", "Desc8", "Loc8", Price.MEDIO, contact, MusicalGenre.ROCK, Zone.ZONA_CABA, null, null);
        productsDao.create(club, "ProdX", "DescX", BigDecimal.valueOf(50), ProductType.ALCOHOLICA, true);
        productsDao.create(club, "ProdY", "DescY", BigDecimal.valueOf(60), ProductType.NO_ALCOHOLICA, true);

        List<Product> products = productsDao.findByClubID(club.getClubId());
        assertEquals(2, products.size());
    }
}
*/