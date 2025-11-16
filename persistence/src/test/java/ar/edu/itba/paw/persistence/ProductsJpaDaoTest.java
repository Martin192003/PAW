package ar.edu.itba.paw.persistence;

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
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@Transactional
public class ProductsJpaDaoTest {

    @Autowired
    private ProductsJpaDao productsDao;

    @PersistenceContext
    private EntityManager em;

    private Club club;
    private Product product;

    @Before
    public void setUp() {
        em.createQuery("DELETE FROM Product p").executeUpdate();
        em.createQuery("DELETE FROM Club c").executeUpdate();
        em.flush();

        club = persistClub("Club Test");
        product = persistProduct(club);

        em.flush();
    }

    // ========================================================================
    // CREATE
    // ========================================================================

    @Test
    public void testCreate() {
        Product p = productsDao.create(
                club,
                "NewProduct",
                "Description",
                new BigDecimal("50.00"),
                ProductType.TRAGOS,
                true
        );
        em.flush();

        assertNotNull(p);
        assertNotNull(p.getProductId());
        assertEquals("NewProduct", p.getProductName());
        assertEquals(club.getClubId(), p.getClub().getClubId());
        assertEquals(0, new BigDecimal("50.00").compareTo(p.getBasePrice()));
        assertEquals(ProductType.TRAGOS, p.getProductType());
        assertTrue(p.getIsAvailable());
        assertEquals(2L, countProducts());
    }

    @Test
    public void testCreateNullClubThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.create(null, "Name", "desc", BigDecimal.ONE, ProductType.TRAGOS, true)
        );
    }

    @Test
    public void testCreateNullClubIdThrows() {
        Club c2 = new Club(); // sin ID
        assertThrows(NullPointerException.class, () ->
                productsDao.create(c2, "Name", "desc", BigDecimal.ONE, ProductType.TRAGOS, true)
        );
    }

    @Test
    public void testCreateNullNameThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.create(club, null, "desc", BigDecimal.ONE, ProductType.TRAGOS, true)
        );
    }

    @Test
    public void testCreateNullBasePriceThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.create(club, "Prod", "desc", null, ProductType.TRAGOS, true)
        );
    }

    @Test
    public void testCreateNegativeBasePriceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                productsDao.create(club, "Prod", "desc", new BigDecimal("-1.00"), ProductType.TRAGOS, true)
        );
    }

    @Test
    public void testCreateNullTypeThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.create(club, "Prod", "desc", BigDecimal.ONE, null, true)
        );
    }

    // ========================================================================
    // UPDATE
    // ========================================================================

    @Test
    public void testUpdate() {
        UUID id = product.getProductId();

        Product p = productsDao.update(
                id,
                club.getClubId(),
                "UpdatedName",
                "NewDesc",
                new BigDecimal("99.99"),
                ProductType.TRAGOS,
                false
        );
        em.flush();

        assertNotNull(p);
        assertEquals("UpdatedName", p.getProductName());
        assertEquals(0, new BigDecimal("99.99").compareTo(p.getBasePrice()));
        assertEquals(ProductType.TRAGOS, p.getProductType());
        assertFalse(p.getIsAvailable());
    }

    @Test
    public void testUpdateNotFoundReturnsNull() {
        Product p = productsDao.update(
                UUID.randomUUID(),
                club.getClubId(),
                "Name",
                "desc",
                BigDecimal.ONE,
                ProductType.TRAGOS,
                true
        );
        assertNull(p);
    }

    @Test
    public void testUpdateNullProductIdThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        null,
                        club.getClubId(),
                        "Name",
                        "desc",
                        BigDecimal.ONE,
                        ProductType.TRAGOS,
                        true
                )
        );
    }

    @Test
    public void testUpdateNullClubIdThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        null,
                        "Name",
                        "desc",
                        BigDecimal.ONE,
                        ProductType.TRAGOS,
                        true
                )
        );
    }

    @Test
    public void testUpdateNullNameThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        club.getClubId(),
                        null,
                        "desc",
                        BigDecimal.ONE,
                        ProductType.TRAGOS,
                        true
                )
        );
    }

    @Test
    public void testUpdateNullBasePriceThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        club.getClubId(),
                        "Name",
                        "desc",
                        null,
                        ProductType.TRAGOS,
                        true
                )
        );
    }

    @Test
    public void testUpdateNegativePriceThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        club.getClubId(),
                        "Name",
                        "desc",
                        new BigDecimal("-5"),
                        ProductType.TRAGOS,
                        true
                )
        );
    }

    @Test
    public void testUpdateNullTypeThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        club.getClubId(),
                        "Name",
                        "desc",
                        BigDecimal.ONE,
                        null,
                        true
                )
        );
    }

    @Test
    public void testUpdateNullIsAvailableThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.update(
                        product.getProductId(),
                        club.getClubId(),
                        "Name",
                        "desc",
                        BigDecimal.ONE,
                        ProductType.TRAGOS,
                        null
                )
        );
    }

    // ========================================================================
    // DELETE
    // ========================================================================

    @Test
    public void testDelete() {
        boolean result = productsDao.delete(product.getProductId());
        em.flush();

        assertTrue(result);
        assertEquals(0L, countProducts());
    }

    @Test
    public void testDeleteNotFoundReturnsFalse() {
        assertFalse(productsDao.delete(UUID.randomUUID()));
        assertEquals(1L, countProducts());
    }

    // ========================================================================
    // FIND BY ID
    // ========================================================================

    @Test
    public void testFindById() {
        Optional<Product> found = productsDao.findById(product.getProductId());

        assertTrue(found.isPresent());
        assertEquals(product.getProductId(), found.get().getProductId());
    }

    @Test
    public void testFindByIdNotFound() {
        Optional<Product> found = productsDao.findById(UUID.randomUUID());
        assertFalse(found.isPresent());
    }

    // ========================================================================
    // FIND BY CLUB
    // ========================================================================

    @Test
    public void testFindByClub() {
        List<Product> list = productsDao.findByClub(club);
        assertEquals(1, list.size());
        assertEquals(product.getProductId(), list.getFirst().getProductId());
    }

    @Test
    public void testFindByClubReturnsEmpty() {
        Club c2 = persistClub("Other");
        em.flush();

        assertTrue(productsDao.findByClub(c2).isEmpty());
    }

    @Test
    public void testFindByClubNullClubThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.findByClub(null)
        );
    }

    @Test
    public void testFindByClubNullClubIdThrows() {
        Club c = new Club();
        assertThrows(NullPointerException.class, () ->
                productsDao.findByClub(c)
        );
    }

    // ========================================================================
    // FIND BY CLUB ID
    // ========================================================================

    @Test
    public void testFindByClubId() {
        List<Product> list = productsDao.findByClubID(club.getClubId());
        assertEquals(1, list.size());
    }

    @Test
    public void testFindByClubIdEmpty() {
        assertTrue(productsDao.findByClubID(UUID.randomUUID()).isEmpty());
    }

    @Test
    public void testFindByClubIdNullThrows() {
        assertThrows(NullPointerException.class, () ->
                productsDao.findByClubID(null)
        );
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private Club persistClub(String name) {
        Club c = new Club(
                name,
                "Location",
                BigDecimal.ONE,
                1,
                null,
                "desc",
                null,
                null,
                null,
                null,
                false,
                null
        );
        em.persist(c);
        return c;
    }

    private Product persistProduct(Club club) {
        Product p = new Product();
        p.setProductId(UUID.randomUUID());
        p.setClub(club);
        p.setProductName("Prod1");
        p.setDescription("Desc1");
        p.setBasePrice(BigDecimal.TEN);
        p.setProductType(ProductType.ALCOHOLICA);
        p.setIsAvailable(true);
        em.persist(p);
        return p;
    }

    private long countProducts() {
        return em.createQuery("SELECT COUNT(p) FROM Product p", Long.class).getSingleResult();
    }
}
