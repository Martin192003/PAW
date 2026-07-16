package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.dto.ClubDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.service.ClubLayoutService;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.TableService;
import ar.edu.itba.paw.service.UserService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cubre listClubs/getClub/createClub/updateClub: son los endpoints reescritos en el
 * commit de paginación (colecciones "solo-links" + headers) que hasta ahora no tenían
 * ningún test (ver AUDITORIA_ENTREGA_FINAL.md, punto 24).
 */
public class ClubRestControllerTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    private ClubService clubService;
    private TableService tableService;
    private UserService userService;
    private ClubRestController controller;

    @Before
    public void setUp() {
        SecurityContextHolder.clearContext();
        clubService = mock(ClubService.class);
        tableService = mock(TableService.class);
        userService = mock(UserService.class);
        controller = new ClubRestController(
                clubService,
                tableService,
                userService,
                mock(ClubLayoutService.class),
                mock(PhotoService.class),
                mock(PhotoDtoAssembler.class)
        );

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getRequestUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs"));
        when(uriInfo.getAbsolutePathBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs"));
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testListClubs_ReturnsLinkCollectionWithPaginationHeaders() {
        Club club1 = club(UUID.randomUUID());
        Club club2 = club(UUID.randomUUID());
        when(clubService.countSearch(null, null, null, null, null)).thenReturn(25);
        when(clubService.searchPaginated(null, null, null, null, null, null, 10, 0))
                .thenReturn(List.of(club1, club2));

        Response response = controller.listClubs(
                null, null, null, null, null, null, null, null, 1, 10, mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("25", response.getHeaderString("X-Total-Count"));

        Object entity = response.getEntity();
        assertTrue(entity instanceof List);
        @SuppressWarnings("unchecked")
        List<LinkDto> links = (List<LinkDto>) entity;
        assertEquals(2, links.size());
        assertEquals("item", links.get(0).getRel());
        assertTrue(links.get(0).getHref().contains(club1.getClubId().toString()));

        String linkHeader = response.getHeaderString("Link");
        assertNotNull("con 25 resultados y size=10 debería haber más páginas", linkHeader);
        assertTrue(linkHeader.contains("rel=\"next\""));
    }

    @Test
    public void testListClubs_InvalidOwnerId_ReturnsBadRequest() {
        Response response = controller.listClubs(
                null, "not-a-uuid", null, null, null, null, null, null, 1, 10, mock(SecurityContext.class));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListClubs_WithOwnerId_DelegatesToOwnerSearch() {
        when(clubService.countClubsByUser(OWNER_ID)).thenReturn(5);
        when(clubService.getClubsByUserPaginated(OWNER_ID, 10, 0)).thenReturn(List.of(club(UUID.randomUUID())));

        Response response = controller.listClubs(
                null, OWNER_ID.toString(), null, null, null, null, null, null, 1, 10, mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Si listClubs no hubiera delegado a la búsqueda por owner, countSearch/searchPaginated
        // (sin stubear para este caso) habrían devuelto los valores "nice" por defecto (0 /
        // lista vacía) y este total no coincidiría con el de countClubsByUser.
        assertEquals("5", response.getHeaderString("X-Total-Count"));
    }

    @Test
    public void testGetClub_InvalidUuid_ReturnsBadRequest() {
        Response response = controller.getClub("not-a-uuid", mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetClub_NotFound_Returns404() {
        when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.empty());
        Response response = controller.getClub(CLUB_ID.toString(), mock(SecurityContext.class));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetClub_AnonymousCaller_GetsPublicRepresentation() {
        Club club = club(CLUB_ID);
        club.setContactInfo(user(OWNER_ID));
        club.setCbu("0000003100000000000001");
        when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.of(club));

        SecurityContext anonymous = mock(SecurityContext.class);
        when(anonymous.getUserPrincipal()).thenReturn(null);

        Response response = controller.getClub(CLUB_ID.toString(), anonymous);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof ClubDto);
        ClubDto dto = (ClubDto) response.getEntity();
        assertNull("un caller anónimo no debería ver el CBU del club", dto.getCbu());
        assertEquals("no-cache", response.getHeaderString("Cache-Control"));
        boolean hasBookingsLink = dto.getLinks().stream().anyMatch(l -> "bookings".equals(l.getRel()));
        assertFalse("el link a /bookings (gestión del club) es sensible y no debería aparecer", hasBookingsLink);
    }

    @Test
    public void testGetClub_OwnerCaller_GetsPrivateRepresentation() {
        User owner = user(OWNER_ID);
        Club club = club(CLUB_ID);
        club.setContactInfo(owner);
        club.setCbu("0000003100000000000001");
        when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.of(club));

        SecurityContext ownerContext = principalFor(owner);

        Response response = controller.getClub(CLUB_ID.toString(), ownerContext);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        ClubDto dto = (ClubDto) response.getEntity();
        assertEquals("0000003100000000000001", dto.getCbu());
        assertEquals("private, no-store", response.getHeaderString("Cache-Control"));
        assertEquals("Authorization", response.getHeaderString("Vary"));
    }

    @Test
    public void testCreateClub_MissingRequiredFields_ReturnsBadRequest() {
        ClubDto body = new ClubDto();
        body.setClubName("Solo el nombre");

        Response response = controller.createClub(body, mock(SecurityContext.class));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateClub_Unauthenticated_ReturnsUnauthorized() {
        Response response = controller.createClub(validClubDto(), mock(SecurityContext.class));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateClub_Success_ReturnsCreatedWithLocation() {
        User owner = user(OWNER_ID);
        Club created = club(CLUB_ID);
        when(clubService.create(
                "Mi Club", "Descripcion", "Palermo", Price.MEDIO, owner,
                MusicalGenre.TECHNO, Zone.ZONA_CABA, Boolean.FALSE, "0000003100000000000001", null
        )).thenReturn(created);

        Response response = controller.createClub(validClubDto(), principalFor(owner));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getLocation());
        assertTrue(response.getLocation().toString().contains(CLUB_ID.toString()));
        assertTrue(response.getEntity() instanceof ClubDto);
    }

    @Test
    public void testUpdateClub_InvalidUuid_ReturnsBadRequest() {
        Response response = controller.updateClub("not-a-uuid", validClubDto(), mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateClub_NotFound_Returns404() {
        when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.empty());
        Response response = controller.updateClub(CLUB_ID.toString(), validClubDto(), mock(SecurityContext.class));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testUpdateClub_Success_ReturnsOkWithPrivateCacheHeaders() {
        User admin = user(OWNER_ID);
        Club existing = club(CLUB_ID);
        existing.setContactInfo(admin);
        when(clubService.getClubById(CLUB_ID)).thenReturn(Optional.of(existing));
        when(clubService.update(
                eq(CLUB_ID), anyString(), anyString(), any(), any(), any(),
                anyString(), any(), any(), any(), any(), any()
        )).thenReturn(existing);

        Response response = controller.updateClub(CLUB_ID.toString(), validClubDto(), principalFor(admin));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("private, no-store", response.getHeaderString("Cache-Control"));
        assertEquals("Authorization", response.getHeaderString("Vary"));
    }

    private static ClubDto validClubDto() {
        ClubDto dto = new ClubDto();
        dto.setClubName("Mi Club");
        dto.setDescription("Descripcion");
        dto.setLocation("Palermo");
        dto.setZone(Zone.ZONA_CABA.name());
        dto.setMusicGenre(MusicalGenre.TECHNO.name());
        dto.setPriceRange(Price.MEDIO.name());
        dto.setCbu("0000003100000000000001");
        return dto;
    }

    private static Club club(UUID id) {
        Club club = new Club();
        club.setClubId(id);
        club.setClubName("Club " + id);
        club.setDescription("desc");
        club.setLocation("loc");
        club.setZone(Zone.ZONA_CABA);
        club.setMusicGenre(MusicalGenre.TECHNO);
        club.setPriceRange(Price.MEDIO);
        club.setIsBar(false);
        club.setRating(BigDecimal.ONE);
        club.setQuantityRatings(1);
        club.setCreatedAt(OffsetDateTime.now());
        return club;
    }

    private static User user(UUID id) {
        User user = new User(
                id,
                "user-" + id,
                "Name",
                "Surname",
                id + "@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455",
                Role.USER,
                OffsetDateTime.now(),
                "es"
        );
        user.setVerified(true);
        return user;
    }

    private SecurityContext principalFor(User user) {
        SecurityContext context = mock(SecurityContext.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(user.getEmail());
        when(context.getUserPrincipal()).thenReturn(principal);
        when(userService.findByEmail(user.getEmail())).thenReturn(user);
        return context;
    }
}
