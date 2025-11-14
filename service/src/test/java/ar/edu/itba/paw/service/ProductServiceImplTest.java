package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import java.math.BigDecimal;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class ProductServiceImplTest {

    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final String NAME = "Cerveza IPA";
    private static final String DESCRIPTION = "Cerveza artesanal";
    private static final BigDecimal BASE_PRICE = BigDecimal.valueOf(1500);
    private static final ProductType TYPE = ProductType.ALCOHOLICA;
    private static final boolean IS_AVAILABLE = true;

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductsDao productsDao;

    @Mock
    private ClubService clubService;

    private Club mockClub;
    private Product mockProduct;

    @Before
    public void setUp() {
        mockClub = new Club();
        mockClub.setClubId(CLUB_ID);

        mockProduct = new Product();
        mockProduct.setProductId(PRODUCT_ID);
        mockProduct.setClub(mockClub);
        mockProduct.setProductName(NAME);
        mockProduct.setDescription(DESCRIPTION);
        mockProduct.setBasePrice(BASE_PRICE);
        mockProduct.setProductType(TYPE);
        mockProduct.setIsAvailable(IS_AVAILABLE);
    }

    // ========================
    // CREATE - HAPPY PATH
    // ========================

    @Test
    public void testCreate_Success() {
        // 1. Setup
        Mockito.when(clubService.getClubById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));
        Mockito.when(productsDao.findByClub(Mockito.eq(mockClub)))
                .thenReturn(List.of()); // 0 productos
        Mockito.when(productsDao.create(
                Mockito.eq(mockClub),
                Mockito.eq(NAME),
                Mockito.eq(DESCRIPTION),
                Mockito.eq(BASE_PRICE),
                Mockito.eq(TYPE),
                Mockito.eq(IS_AVAILABLE)
        )).thenReturn(mockProduct);

        // 2. Exercise
        Product result = productService.create(CLUB_ID, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);

        // 3. Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(PRODUCT_ID, result.getProductId());
        Assert.assertEquals(NAME, result.getProductName());
        Assert.assertEquals(DESCRIPTION, result.getDescription());
        Assert.assertEquals(BASE_PRICE, result.getBasePrice());
        Assert.assertEquals(TYPE, result.getProductType());
        Assert.assertEquals(IS_AVAILABLE, result.getIsAvailable());
    }

    @Test
    public void testCreate_ClubNotFound() {
        // 1. Setup
        Mockito.when(clubService.getClubById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        Assert.assertThrows(ClubNotFoundException.class, () -> {
            productService.create(CLUB_ID, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
        });
    }

    @Test
    public void testCreate_PriceTooHigh() {
        // 1. Setup
        Mockito.when(clubService.getClubById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));

        // 2. Exercise + 3. Assert
        Assert.assertThrows(ValidationException.class, () -> {
            productService.create(CLUB_ID, NAME, DESCRIPTION, BigDecimal.valueOf(100000000), TYPE, IS_AVAILABLE);
        });
    }

    @Test
    public void testCreate_NameTooLong() {
        // 1. Setup
        Mockito.when(clubService.getClubById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));

        String longName = "a".repeat(101);

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            productService.create(CLUB_ID, longName, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
        });
    }

    @Test
    public void testCreate_MaxProductsReached() {
        // 1. Setup
        List<Product> fiftyProducts = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Product p = new Product();
            p.setProductId(UUID.randomUUID());
            fiftyProducts.add(p);
        }

        Mockito.when(clubService.getClubById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(mockClub));
        Mockito.when(productsDao.findByClub(Mockito.eq(mockClub)))
                .thenReturn(fiftyProducts);

        // 2. Exercise + 3. Assert
        Assert.assertThrows(ValidationException.class, () -> {
            productService.create(CLUB_ID, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
        });
    }

    // ========================
    // UPDATE - HAPPY PATH
    // ========================

    @Test
    public void testUpdate_Success() {
        // 1. Setup: Producto actualizado con nombre nuevo
        Product updatedProduct = new Product();
        updatedProduct.setProductId(PRODUCT_ID);
        updatedProduct.setProductName("Cerveza IPA Premium");
        updatedProduct.setDescription(DESCRIPTION);
        updatedProduct.setBasePrice(BASE_PRICE);
        updatedProduct.setProductType(TYPE);
        updatedProduct.setIsAvailable(IS_AVAILABLE);

        // 2. Mock: DAO devuelve el producto actualizado
        Mockito.when(productsDao.update(
                Mockito.eq(PRODUCT_ID),
                Mockito.eq(CLUB_ID),
                Mockito.eq("Cerveza IPA Premium"),
                Mockito.eq(DESCRIPTION),
                Mockito.eq(BASE_PRICE),
                Mockito.eq(TYPE),
                Mockito.eq(IS_AVAILABLE)
        )).thenReturn(updatedProduct);

        // 3. Exercise
        Product result = productService.update(
                PRODUCT_ID,
                CLUB_ID,
                "Cerveza IPA Premium",
                DESCRIPTION,
                BASE_PRICE,
                TYPE,
                IS_AVAILABLE
        );

        // 4. Asserts
        Assert.assertNotNull(result);
        Assert.assertEquals(PRODUCT_ID, result.getProductId());
        Assert.assertEquals("Cerveza IPA Premium", result.getProductName());
        Assert.assertEquals(DESCRIPTION, result.getDescription());
        Assert.assertEquals(BASE_PRICE, result.getBasePrice());
        Assert.assertEquals(TYPE, result.getProductType());
        Assert.assertEquals(IS_AVAILABLE, result.getIsAvailable());
    }

    @Test
    public void testUpdate_PriceTooHigh() {

        // 2. Exercise + 3. Assert
        Assert.assertThrows(ValidationException.class, () -> {
            productService.update(PRODUCT_ID, CLUB_ID, NAME, DESCRIPTION,
                    BigDecimal.valueOf(100000000), TYPE, IS_AVAILABLE);
        });
    }

    // ========================
    // DELETE
    // ========================

    @Test
    public void testDelete_Success() {
        // 1. Setup
        Mockito.when(productsDao.delete(Mockito.eq(PRODUCT_ID)))
                .thenReturn(true);

        // 2. Exercise
        boolean result = productService.delete(PRODUCT_ID);

        // 3. Assert
        Assert.assertTrue(result);
    }

    // ========================
    // FIND BY ID
    // ========================

    @Test
    public void testFindById_Exists() {
        Mockito.when(productsDao.findById(Mockito.eq(PRODUCT_ID)))
               .thenReturn(Optional.of(mockProduct));

        Optional<Product> result = productService.findById(PRODUCT_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(mockProduct, result.get());
    }

    @Test
    public void testFindById_NotExists() {
        Mockito.when(productsDao.findById(Mockito.eq(PRODUCT_ID)))
               .thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(PRODUCT_ID);

        Assert.assertFalse(result.isPresent());
    }

    // ========================
    // FIND BY CLUB
    // ========================

    @Test
    public void testFindByClub() {
        // 1. Setup: Dos productos reales
        Product p1 = new Product();
        p1.setProductId(UUID.randomUUID());
        p1.setProductName("Cerveza IPA");
        p1.setIsAvailable(true);

        Product p2 = new Product();
        p2.setProductId(UUID.randomUUID());
        p2.setProductName("Fernet");
        p2.setIsAvailable(true);

        // 2. Mock: DAO devuelve la lista
        Mockito.when(productsDao.findByClub(Mockito.eq(mockClub)))
                .thenReturn(Arrays.asList(p1, p2));

        // 3. Exercise
        List<Product> result = productService.findByClub(mockClub);

        // 4. Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(p1));
        Assert.assertTrue(result.contains(p2));
    }

    @Test
    public void testFindByClubID() {
        Product p1 = new Product(); p1.setProductId(UUID.randomUUID());
        Product p2 = new Product(); p2.setProductId(UUID.randomUUID());

        Mockito.when(productsDao.findByClubID(Mockito.eq(CLUB_ID)))
               .thenReturn(Arrays.asList(p1, p2));

        List<Product> result = productService.findByClubID(CLUB_ID);

        Assert.assertEquals(2, result.size());
    }

    // ========================
    // CAN ADD PRODUCT
    // ========================

    @Test
    public void testCanAddProduct_True() {
        Mockito.when(productsDao.findByClub(Mockito.eq(mockClub)))
               .thenReturn(List.of()); // 0 productos

        boolean result = productService.canAddProduct(mockClub);

        Assert.assertTrue(result);
    }

    @Test
    public void testCanAddProduct_False_MaxReached() {
        List<Product> fifty = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fifty.add(new Product());
        }

        Mockito.when(productsDao.findByClub(Mockito.eq(mockClub)))
               .thenReturn(fifty);

        boolean result = productService.canAddProduct(mockClub);

        Assert.assertFalse(result);
    }

    @Test
    public void testCanAddProduct_False_NullClub() {
        boolean result = productService.canAddProduct(null);
        Assert.assertFalse(result);
    }

    // ========================
    // CLASSIFY CATEGORY
    // ========================

    @Test
    public void testClassifyCategory_Cerveza() {
        Product p = new Product();
        p.setProductName("IPA Artesanal");
        p.setProductType(ProductType.ALCOHOLICA);

        String category = productService.classifyCategory(p);

        Assert.assertEquals("CERVEZAS", category);
    }

    @Test
    public void testClassifyCategory_Champagne() {
        Product p = new Product();
        p.setProductName("Champagne Brut");
        p.setProductType(ProductType.ALCOHOLICA);

        String category = productService.classifyCategory(p);

        Assert.assertEquals("CHAMPAGNE", category);
    }

    @Test
    public void testClassifyCategory_NoAlcoholica() {
        Product p = new Product();
        p.setProductName("Agua Mineral");
        p.setProductType(ProductType.NO_ALCOHOLICA);

        String category = productService.classifyCategory(p);

        Assert.assertEquals("NO_ALCOHOLICA", category);
    }

    @Test
    public void testClassifyCategory_DefaultTragos() {
        Product p = new Product();
        p.setProductName("Fernet con Coca");
        p.setProductType(ProductType.TRAGOS);

        String category = productService.classifyCategory(p);

        Assert.assertEquals("TRAGOS", category);
    }

    // ========================
    // GET VISIBLE PRODUCTS BY CATEGORY
    // ========================

    @Test
    public void testGetVisibleProductsByCategory() {
        Product cerveza = new Product();
        cerveza.setProductName("IPA");
        cerveza.setIsAvailable(true);

        Product trago = new Product();
        trago.setProductName("Gin Tonic");
        trago.setIsAvailable(true);

        Product noDisponible = new Product();
        noDisponible.setProductName("Vino");
        noDisponible.setIsAvailable(false);

        Mockito.when(productsDao.findByClubID(Mockito.eq(CLUB_ID)))
                .thenReturn(Arrays.asList(cerveza, trago, noDisponible));

        Map<String, List<Product>> result = productService.getVisibleProductsByCategory(CLUB_ID);

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsKey("CERVEZAS"));
        Assert.assertTrue(result.containsKey("TRAGOS"));
        Assert.assertEquals(1, result.get("CERVEZAS").size());
        Assert.assertEquals(1, result.get("TRAGOS").size());
    }
}

