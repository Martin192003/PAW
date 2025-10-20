package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class ProductServiceImplTest {
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final String NAME = "ProductoTest";
    private static final String DESCRIPTION = "Descripción";
    private static final BigDecimal BASE_PRICE = BigDecimal.valueOf(1000);
    private static final ProductType TYPE = ProductType.ALCOHOLICA;
    private static final boolean IS_AVAILABLE = true;

    @Mock
    private ProductsDao productsDao;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    public void testCreateSuccess() {
        Club club = Mockito.mock(Club.class);
        Product product = Mockito.mock(Product.class);
        Mockito.when(productsDao.create(club, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE)).thenReturn(product);
        Product result = productService.create(club, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
        Assert.assertEquals(product, result);
    }

    @Test(expected = ValidationException.class)
    public void testCreatePriceTooHigh() {
        Club club = Mockito.mock(Club.class);
        productService.create(club, NAME, DESCRIPTION, BigDecimal.valueOf(100000000), TYPE, IS_AVAILABLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNameTooLong() {
        Club club = Mockito.mock(Club.class);
        String longName = "a".repeat(101);
        productService.create(club, longName, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
    }

    @Test
    public void testUpdateSuccess() {
        Product product = Mockito.mock(Product.class);
        Mockito.when(productsDao.update(PRODUCT_ID, CLUB_ID, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE)).thenReturn(product);
        Product result = productService.update(PRODUCT_ID, CLUB_ID, NAME, DESCRIPTION, BASE_PRICE, TYPE, IS_AVAILABLE);
        Assert.assertEquals(product, result);
    }

    @Test(expected = ValidationException.class)
    public void testUpdatePriceTooHigh() {
        productService.update(PRODUCT_ID, CLUB_ID, NAME, DESCRIPTION, BigDecimal.valueOf(100000000), TYPE, IS_AVAILABLE);
    }

    @Test
    public void testDelete() {
        Mockito.when(productsDao.delete(PRODUCT_ID)).thenReturn(true);
        boolean result = productService.delete(PRODUCT_ID);
        Assert.assertTrue(result);
    }

    @Test
    public void testFindById() {
        Product product = Mockito.mock(Product.class);
        Mockito.when(productsDao.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        Optional<Product> result = productService.findById(PRODUCT_ID);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(product, result.get());
    }

    @Test
    public void testFindByName() {
        Product product = Mockito.mock(Product.class);
        Mockito.when(productsDao.findByName(NAME)).thenReturn(Optional.of(product));
        Optional<Product> result = productService.findByName(NAME);
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(product, result.get());
    }


    @Test
    public void testFindByClub() {
        Club club = Mockito.mock(Club.class);
        Product product1 = Mockito.mock(Product.class);
        Product product2 = Mockito.mock(Product.class);
        Mockito.when(productsDao.findByClub(club)).thenReturn(Arrays.asList(product1, product2));
        List<Product> result = productService.findByClub(club);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
    }

    @Test
    public void testFindByClubID() {
        Product product1 = Mockito.mock(Product.class);
        Product product2 = Mockito.mock(Product.class);
        Mockito.when(productsDao.findByClubID(CLUB_ID)).thenReturn(Arrays.asList(product1, product2));
        List<Product> result = productService.findByClubID(CLUB_ID);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
    }
}

