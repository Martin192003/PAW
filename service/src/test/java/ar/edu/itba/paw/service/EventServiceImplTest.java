package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.persistence.EventDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceImplTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID OTHER_CLUB_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID PHOTO_ID = UUID.randomUUID();
    private static final UUID NEW_PHOTO_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HARDCODED_UPLOADER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final String EVENT_NAME = "Fiesta Electrónica";
    private static final String DESCRIPTION = "La mejor fiesta del finde";
    private static final LocalDate FUTURE_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate PAST_DATE = LocalDate.now().minusDays(1);

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private EventDao eventDao;

    @Mock
    private PhotoService photoService;

    @Mock
    private SecurityService securityService;

    private Event mockEvent;
    private Event mockUpdatedEvent;
    private User mockUser;
    private Photo mockPhoto;

    @Before
    public void setUp() {
        mockEvent = Mockito.mock(Event.class);
        mockUpdatedEvent = Mockito.mock(Event.class);
        mockUser = Mockito.mock(User.class);

        mockPhoto = new Photo();
        mockPhoto.setFilename("event.jpg");
        mockPhoto.setContentType("image/jpeg");
        mockPhoto.setData(new byte[]{1, 2, 3});

        Mockito.when(mockEvent.getEventId()).thenReturn(EVENT_ID);
        Mockito.when(mockEvent.getClubId()).thenReturn(CLUB_ID);
        Mockito.when(mockEvent.getPhotoId()).thenReturn(PHOTO_ID);

        Mockito.when(mockUser.getId()).thenReturn(USER_ID);
    }

    @Test
    public void testCreate_Success_TrimsFields() {
        Mockito.when(eventDao.create(
                Mockito.eq(CLUB_ID),
                Mockito.eq("Fiesta Electrónica"),
                Mockito.eq("Descripción con espacios"),
                Mockito.eq(FUTURE_DATE)
        )).thenReturn(mockEvent);

        Event result = eventService.create(
                CLUB_ID,
                "  Fiesta Electrónica  ",
                "  Descripción con espacios  ",
                FUTURE_DATE
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(mockEvent, result);
    }

    @Test
    public void testCreate_Success_NullDescriptionWhenBlank() {
        Mockito.when(eventDao.create(
                Mockito.eq(CLUB_ID),
                Mockito.eq(EVENT_NAME),
                Mockito.isNull(),
                Mockito.eq(FUTURE_DATE)
        )).thenReturn(mockEvent);

        Event result = eventService.create(CLUB_ID, EVENT_NAME, "   ", FUTURE_DATE);

        Assert.assertNotNull(result);
        Assert.assertEquals(mockEvent, result);
    }

    @Test
    public void testCreate_InvalidName() {
        Assert.assertThrows(ValidationException.class, () ->
                eventService.create(CLUB_ID, "   ", DESCRIPTION, FUTURE_DATE)
        );
    }

    @Test
    public void testCreate_InvalidPastDate() {
        Assert.assertThrows(InvalidDateRangeException.class, () ->
                eventService.create(CLUB_ID, EVENT_NAME, DESCRIPTION, PAST_DATE)
        );
    }

    @Test
    public void testUpdate_Success_WithImage() throws IOException {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(securityService.getVerifiedLoggedUser())
                .thenReturn(mockUser);
        Mockito.when(photoService.uploadEventPhoto(
                Mockito.eq(CLUB_ID),
                Mockito.eq(EVENT_ID),
                Mockito.eq(USER_ID),
                Mockito.eq(mockPhoto)
        )).thenReturn(NEW_PHOTO_ID);
        Mockito.when(eventDao.update(
                Mockito.eq(EVENT_ID),
                Mockito.eq(CLUB_ID),
                Mockito.eq(EVENT_NAME),
                Mockito.eq(DESCRIPTION),
                Mockito.eq(FUTURE_DATE),
                Mockito.eq(NEW_PHOTO_ID)
        )).thenReturn(mockUpdatedEvent);

        Event result = eventService.update(
                CLUB_ID,
                EVENT_ID,
                EVENT_NAME,
                DESCRIPTION,
                FUTURE_DATE,
                mockPhoto
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(mockUpdatedEvent, result);
    }

    @Test
    public void testUpdate_Success_WithoutImage_KeepsPreviousPhoto() throws IOException {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(eventDao.update(
                Mockito.eq(EVENT_ID),
                Mockito.eq(CLUB_ID),
                Mockito.eq(EVENT_NAME),
                Mockito.isNull(),
                Mockito.eq(FUTURE_DATE),
                Mockito.eq(PHOTO_ID)
        )).thenReturn(mockUpdatedEvent);
        // En vez de un Mockito.verify(never()) posterior, dejamos que el propio stub
        // detecte una llamada inesperada: si update() subiera una foto nueva pese a que
        // no se pasó imagen, este stub explota durante el "ejercitar". lenient() porque,
        // en el camino feliz, este stub no llega a usarse (y eso es lo esperado): sin
        // lenient(), Mockito en modo strict marcaría el stub como "unnecessary".
        Mockito.lenient().when(photoService.uploadEventPhoto(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new AssertionError("photoService.uploadEventPhoto should not have been called"));

        Event result = eventService.update(
                CLUB_ID,
                EVENT_ID,
                EVENT_NAME,
                "   ",
                FUTURE_DATE,
                null
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(mockUpdatedEvent, result);
    }

    @Test
    public void testUpdate_EventNotFoundInFindById() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.empty());

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.update(CLUB_ID, EVENT_ID, EVENT_NAME, DESCRIPTION, FUTURE_DATE, null)
        );
    }

    @Test
    public void testUpdate_InvalidClub() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getClubId()).thenReturn(OTHER_CLUB_ID);

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.update(CLUB_ID, EVENT_ID, EVENT_NAME, DESCRIPTION, FUTURE_DATE, null)
        );
    }

    @Test
    public void testUpdate_NotFoundAfterDaoUpdate() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(eventDao.update(
                Mockito.eq(EVENT_ID),
                Mockito.eq(CLUB_ID),
                Mockito.eq(EVENT_NAME),
                Mockito.eq(DESCRIPTION),
                Mockito.eq(FUTURE_DATE),
                Mockito.eq(PHOTO_ID)
        )).thenReturn(null);

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.update(CLUB_ID, EVENT_ID, EVENT_NAME, DESCRIPTION, FUTURE_DATE, null)
        );
    }

    @Test
    public void testDelete_Success_WithPhoto() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        // Si eventDao.delete no se llamara con el id correcto, el mock "nice" por
        // defecto devuelve false y delete() lanzaría NotFoundException -> el test ya
        // fallaría por una excepción inesperada, sin necesitar un verify() aparte.
        Mockito.when(eventDao.delete(Mockito.eq(EVENT_ID)))
                .thenReturn(true);

        // photoService.delete es void: capturamos si el stub llegó a ejecutarse (y con
        // qué id) en vez de usar Mockito.verify() después de ejercitar.
        AtomicBoolean photoDeleted = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            UUID deletedId = invocation.getArgument(0);
            if (!PHOTO_ID.equals(deletedId)) {
                throw new AssertionError("photoService.delete called with unexpected id " + deletedId);
            }
            photoDeleted.set(true);
            return null;
        }).when(photoService).delete(Mockito.any());

        eventService.delete(CLUB_ID, EVENT_ID);

        Assert.assertTrue("se esperaba que se borrara la foto asociada al evento", photoDeleted.get());
    }

    @Test
    public void testDelete_Success_WithoutPhoto() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getPhotoId()).thenReturn(null);
        Mockito.when(eventDao.delete(Mockito.eq(EVENT_ID)))
                .thenReturn(true);
        // Si delete() igualmente intentara borrar una foto que no existe, este stub
        // explota durante el "ejercitar" en vez de comprobarse después con verify().
        // lenient() porque en el camino feliz este stub no se usa, y eso es lo esperado.
        Mockito.lenient().doThrow(new AssertionError("photoService.delete should not have been called"))
                .when(photoService).delete(Mockito.any());

        eventService.delete(CLUB_ID, EVENT_ID);
    }

    @Test
    public void testDelete_InvalidClub() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getClubId()).thenReturn(OTHER_CLUB_ID);

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.delete(CLUB_ID, EVENT_ID)
        );
    }

    @Test
    public void testGet_Success() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));

        Event result = eventService.get(EVENT_ID);

        Assert.assertNotNull(result);
        Assert.assertEquals(mockEvent, result);
    }

    @Test
    public void testGet_NotFound() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.empty());

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.get(EVENT_ID)
        );
    }

    @Test
    public void testListByClub_Success() {
        List<Event> events = Arrays.asList(mockEvent);
        Mockito.when(eventDao.findByClub(Mockito.eq(CLUB_ID), Mockito.eq(0), Mockito.eq(10)))
                .thenReturn(events);

        List<Event> result = eventService.listByClub(CLUB_ID, 0, 10);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockEvent));
    }

    @Test
    public void testListByClub_InvalidPaging() {
        Assert.assertThrows(ValidationException.class, () ->
                eventService.listByClub(CLUB_ID, -1, 10)
        );

        Assert.assertThrows(ValidationException.class, () ->
                eventService.listByClub(CLUB_ID, 0, 0)
        );
    }

    @Test
    public void testCountByClub() {
        Mockito.when(eventDao.countByClub(Mockito.eq(CLUB_ID))).thenReturn(7);

        int result = eventService.countByClub(CLUB_ID);

        Assert.assertEquals(7, result);
    }

    @Test
    public void testListUpcoming_Success() {
        List<Event> events = Arrays.asList(mockEvent);
        Mockito.when(eventDao.findUpcoming(Mockito.any(LocalDate.class), Mockito.eq(0), Mockito.eq(5)))
                .thenReturn(events);

        List<Event> result = eventService.listUpcoming(0, 5);

        Assert.assertEquals(1, result.size());
        Assert.assertTrue(result.contains(mockEvent));
    }

    @Test
    public void testCountUpcoming() {
        Mockito.when(eventDao.countUpcoming(Mockito.any(LocalDate.class))).thenReturn(4);

        int result = eventService.countUpcoming();

        Assert.assertEquals(4, result);
    }

    @Test
    public void testTotalEventPagesPast() {
        Mockito.when(eventDao.countPastByClub(Mockito.eq(CLUB_ID))).thenReturn(11);

        int result = eventService.totalEventPagesPast(CLUB_ID, 5);

        Assert.assertEquals(3, result);
    }

    @Test
    public void testTotalEventPagesUpcoming() {
        Mockito.when(eventDao.countUpcomingByClub(Mockito.eq(CLUB_ID))).thenReturn(9);

        int result = eventService.totalEventPagesUpcoming(CLUB_ID, 4);

        Assert.assertEquals(3, result);
    }

    @Test
    public void testTotalEventPages() {
        Mockito.when(eventDao.countByClub(Mockito.eq(CLUB_ID))).thenReturn(12);

        int result = eventService.totalEventPages(CLUB_ID, 5);

        Assert.assertEquals(3, result);
    }


    @Test
    public void testSetEventPhoto_InvalidImageData() {
        Photo invalidPhoto = new Photo();
        invalidPhoto.setFilename("event.jpg");
        invalidPhoto.setContentType("image/jpeg");
        invalidPhoto.setData(new byte[]{});

        Assert.assertThrows(ValidationException.class, () ->
                eventService.setEventPhoto(CLUB_ID, EVENT_ID, invalidPhoto)
        );
    }

    @Test
    public void testSetEventPhoto_InvalidFilename() {
        Photo invalidPhoto = new Photo();
        invalidPhoto.setFilename("   ");
        invalidPhoto.setContentType("image/jpeg");
        invalidPhoto.setData(new byte[]{1, 2, 3});

        Assert.assertThrows(ValidationException.class, () ->
                eventService.setEventPhoto(CLUB_ID, EVENT_ID, invalidPhoto)
        );
    }

    @Test
    public void testSetEventPhoto_InvalidContentType() {
        Photo invalidPhoto = new Photo();
        invalidPhoto.setFilename("event.jpg");
        invalidPhoto.setContentType("   ");
        invalidPhoto.setData(new byte[]{1, 2, 3});

        Assert.assertThrows(ValidationException.class, () ->
                eventService.setEventPhoto(CLUB_ID, EVENT_ID, invalidPhoto)
        );
    }

    @Test
    public void testDeleteEventPhoto_Success() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));

        // deleteEventPhoto es void y sus tres efectos son llamadas a colaboradores; en
        // vez de Mockito.verify() después, cada stub confirma su propia invocación (y
        // argumentos correctos) mientras se ejercita el método.
        AtomicBoolean photoDetached = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            Object newPhotoId = invocation.getArgument(1);
            if (!EVENT_ID.equals(id) || newPhotoId != null) {
                throw new AssertionError("eventDao.attachPhoto called with unexpected arguments");
            }
            photoDetached.set(true);
            return null;
        }).when(eventDao).attachPhoto(Mockito.any(), Mockito.any());

        AtomicBoolean photoDeleted = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            if (!PHOTO_ID.equals(id)) {
                throw new AssertionError("photoService.delete called with unexpected id " + id);
            }
            photoDeleted.set(true);
            return null;
        }).when(photoService).delete(Mockito.any());

        AtomicBoolean photoIdCleared = new AtomicBoolean(false);
        Mockito.doAnswer(invocation -> {
            photoIdCleared.set(true);
            return null;
        }).when(mockEvent).setPhotoId(null);

        eventService.deleteEventPhoto(CLUB_ID, EVENT_ID);

        Assert.assertTrue("se esperaba que se desvinculara la foto del evento en el DAO", photoDetached.get());
        Assert.assertTrue("se esperaba que se borrara la foto vieja", photoDeleted.get());
        Assert.assertTrue("se esperaba que se limpiara el photoId en memoria", photoIdCleared.get());
    }

    @Test
    public void testDeleteEventPhoto_NoPhoto_DoesNothing() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getPhotoId()).thenReturn(null);

        // deleteEventPhoto hace un return temprano cuando el evento no tiene foto; si esa
        // guarda fallara, cualquiera de estos dos stubs lanzaría un AssertionError
        // durante el "ejercitar", sin necesitar verify(never()) después. lenient() porque
        // en el camino feliz (guarda intacta) ninguno de los dos llega a usarse.
        Mockito.lenient().doThrow(new AssertionError("eventDao.attachPhoto should not have been called"))
                .when(eventDao).attachPhoto(Mockito.any(), Mockito.any());
        Mockito.lenient().doThrow(new AssertionError("photoService.delete should not have been called"))
                .when(photoService).delete(Mockito.any());

        eventService.deleteEventPhoto(CLUB_ID, EVENT_ID);
    }

    @Test
    public void testGetEventPhotoId_Success() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));

        Optional<UUID> result = eventService.getEventPhotoId(CLUB_ID, EVENT_ID);

        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(PHOTO_ID, result.get());
    }

    @Test
    public void testGetEventPhotoId_NoPhoto() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getPhotoId()).thenReturn(null);

        Optional<UUID> result = eventService.getEventPhotoId(CLUB_ID, EVENT_ID);

        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testGetEventPhoto_Success() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(photoService.getEventPhoto(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockPhoto));

        Photo result = eventService.getEventPhoto(CLUB_ID, EVENT_ID);

        Assert.assertNotNull(result);
        Assert.assertEquals(mockPhoto, result);
    }

    @Test
    public void testGetEventPhoto_NotFoundBecauseEventHasNoPhoto() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(mockEvent.getPhotoId()).thenReturn(null);

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.getEventPhoto(CLUB_ID, EVENT_ID)
        );
    }

    @Test
    public void testGetEventPhoto_NotFoundInPhotoService() {
        Mockito.when(eventDao.findById(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.of(mockEvent));
        Mockito.when(photoService.getEventPhoto(Mockito.eq(EVENT_ID)))
                .thenReturn(Optional.empty());

        Assert.assertThrows(NotFoundException.class, () ->
                eventService.getEventPhoto(CLUB_ID, EVENT_ID)
        );
    }
}
