package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ProductsDao;
import ar.edu.itba.paw.service.MenuServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class MenuServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();

    @Mock
    private ProductsDao productsDao;

    @Mock
    private ClubDao clubDao;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    public void testGetMenuByClub_Success() {
        // 1. Setup
        Club club = new Club();
        club.setClubId(CLUB_ID);

        Product product1 = new Product();
        product1.setProductName("Pizza");

        Product product2 = new Product();
        product2.setProductName("Hamburguesa");

        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.of(club));
        Mockito.when(productsDao.findByClub(Mockito.eq(club)))
                .thenReturn(Arrays.asList(product1, product2));

        // 2. Exercise
        List<Product> menu = menuService.getMenuByClub(CLUB_ID);

        // 3. Assert
        Assert.assertNotNull(menu);
        Assert.assertEquals(2, menu.size());
        Assert.assertEquals("Pizza", menu.get(0).getProductName());
        Assert.assertEquals("Hamburguesa", menu.get(1).getProductName());
    }

    @Test
    public void testGetMenuByClub_ClubNotFound() {
        // 1. Setup
        Mockito.when(clubDao.findById(Mockito.eq(CLUB_ID)))
                .thenReturn(Optional.empty());

        // 2. Exercise + 3. Assert
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            menuService.getMenuByClub(CLUB_ID);
        });
    }
}