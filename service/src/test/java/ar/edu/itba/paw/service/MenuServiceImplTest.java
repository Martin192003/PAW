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
    private static final String CLUB_NAME = "ClubTest";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.now();

    @Mock
    private ProductsDao productsDao;

    @Mock
    private ClubDao clubDao;

    @InjectMocks
    private MenuServiceImpl menuService;

    @Test
    public void testGetMenuByClub() {
        Club club = new Club(CLUB_ID, CLUB_NAME, "UbicaciÃ³n", BigDecimal.valueOf(4.5), 10, Price.MEDIO, "desc", null, MusicalGenre.ROCK, CREATED_AT, Zone.ZONA_CABA);
        Product product1 = new Product(UUID.randomUUID(), club, "Pizza", "desc1", BigDecimal.valueOf(100), null, true, CREATED_AT);
        Product product2 = new Product(UUID.randomUUID(), club, "Hamburguesa", "desc2", BigDecimal.valueOf(150), null, true, CREATED_AT);

        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.of(club));
        Mockito.when(productsDao.findByClub(club)).thenReturn(Arrays.asList(product1, product2));

        List<Product> menu = menuService.getMenuByClub(CLUB_ID);

        Assert.assertNotNull(menu);
        Assert.assertEquals(2, menu.size());
        Assert.assertEquals("Pizza", menu.get(0).getProductName());
        Assert.assertEquals("Hamburguesa", menu.get(1).getProductName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMenuByClubClubNotFound() {
        Mockito.when(clubDao.findById(CLUB_ID)).thenReturn(Optional.empty());
        menuService.getMenuByClub(CLUB_ID);
    }
}