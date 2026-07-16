package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.persistence.ClubLayoutDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(MockitoJUnitRunner.class)
public class ClubLayoutServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final String ADMIN_EMAIL = "admin@club.com";
    private static final String FILENAME = "layout.png";
    private static final byte[] DATA = new byte[]{1, 2, 3, 4};

    @Mock private ClubLayoutDao layoutDao;
    @Mock private UserService userService;
    @Mock private ClubService clubService;

    @InjectMocks private ClubLayoutServiceImpl clubLayoutService;

    private User adminUser;

    @Before
    public void setUp() {
        adminUser = new User(ADMIN_ID);
        Mockito.when(userService.findByEmail(ADMIN_EMAIL)).thenReturn(adminUser);
    }

    @Test
    public void testUploadLayoutSuccess_DefaultsMissingContentType() {
        ClubLayout[] savedLayout = new ClubLayout[1];
        // layoutDao.save recibe el layout recién construido (variable local del método
        // bajo test, no accesible desde afuera); en vez de un Mockito.verify() posterior,
        // lo capturamos con thenAnswer y afirmamos sobre sus campos.
        Mockito.when(layoutDao.save(Mockito.any())).thenAnswer(invocation -> {
            savedLayout[0] = invocation.getArgument(0);
            return savedLayout[0];
        });

        clubLayoutService.uploadLayout(CLUB_ID, ADMIN_EMAIL, FILENAME, null, DATA);

        Assert.assertNotNull("uploadLayout debería haber guardado un layout", savedLayout[0]);
        Assert.assertEquals(CLUB_ID, savedLayout[0].getClubId());
        Assert.assertEquals(ADMIN_ID, savedLayout[0].getUploadedBy());
        Assert.assertEquals(FILENAME, savedLayout[0].getFilename());
        Assert.assertArrayEquals(DATA, savedLayout[0].getData());
        Assert.assertEquals("application/octet-stream", savedLayout[0].getContentType());
        Assert.assertEquals(Long.valueOf(DATA.length), savedLayout[0].getSizeBytes());
        Assert.assertNotNull(savedLayout[0].getLayoutId());
        Assert.assertTrue(savedLayout[0].getUrl().contains(CLUB_ID.toString()));
    }

    @Test
    public void testUploadLayoutSuccess_KeepsExplicitContentType() {
        ClubLayout[] savedLayout = new ClubLayout[1];
        Mockito.when(layoutDao.save(Mockito.any())).thenAnswer(invocation -> {
            savedLayout[0] = invocation.getArgument(0);
            return savedLayout[0];
        });

        clubLayoutService.uploadLayout(CLUB_ID, ADMIN_EMAIL, FILENAME, "image/png", DATA);

        Assert.assertEquals("image/png", savedLayout[0].getContentType());
    }

    @Test
    public void testUploadLayoutDeletesPreviousLayoutBeforeSaving() {
        // club_layouts tiene un constraint unique en club_id: si save() se llamara antes
        // que deleteByClubId(), o si deleteByClubId no se llamara, el insert real
        // fallaría. En vez de comprobar el orden con Mockito.verify() (InOrder), usamos
        // un flag: el stub de save() explota si todavía no se borró el layout anterior.
        AtomicBoolean deletedFirst = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            if (!CLUB_ID.equals(id)) {
                throw new AssertionError("deleteByClubId called with unexpected clubId");
            }
            deletedFirst.set(true);
            return null;
        }).when(layoutDao).deleteByClubId(Mockito.any());

        Mockito.when(layoutDao.save(Mockito.any())).thenAnswer(invocation -> {
            if (!deletedFirst.get()) {
                throw new AssertionError("layoutDao.save called before deleteByClubId");
            }
            return invocation.getArgument(0);
        });

        clubLayoutService.uploadLayout(CLUB_ID, ADMIN_EMAIL, FILENAME, "image/png", DATA);

        Assert.assertTrue("se esperaba que se borrara el layout anterior antes de guardar el nuevo",
                deletedFirst.get());
    }

    @Test
    public void testUploadLayoutNotAdminThrows() {
        Mockito.doThrow(new UnauthorizedException())
                .when(clubService).assertAdmin(CLUB_ID, ADMIN_ID);
        // clubService.assertAdmin corta el flujo antes de llegar a layoutDao.save: si esa
        // guarda se rompiera, uploadLayout no lanzaría UnauthorizedException y este
        // assertThrows ya lo detectaría, sin necesidad de un stub adicional sobre save()
        // (que Mockito en modo strict marcaría como "unnecessary stubbing" al no
        // ejecutarse nunca).
        Assert.assertThrows(UnauthorizedException.class, () ->
                clubLayoutService.uploadLayout(CLUB_ID, ADMIN_EMAIL, FILENAME, "image/png", DATA));
    }

    @Test
    public void testGetLayoutByClubDelegatesToDao() {
        ClubLayout layout = new ClubLayout();
        layout.setClubId(CLUB_ID);
        Mockito.when(layoutDao.findByClubId(CLUB_ID)).thenReturn(Optional.of(layout));

        Optional<ClubLayout> result = clubLayoutService.getLayoutByClub(CLUB_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(CLUB_ID, result.get().getClubId());
    }

    @Test
    public void testGetLayoutByClubNotFound() {
        Mockito.when(layoutDao.findByClubId(CLUB_ID)).thenReturn(Optional.empty());

        Optional<ClubLayout> result = clubLayoutService.getLayoutByClub(CLUB_ID);

        Assert.assertFalse(result.isPresent());
    }
}
