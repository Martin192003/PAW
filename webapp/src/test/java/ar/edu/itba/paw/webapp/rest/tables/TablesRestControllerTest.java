package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.TableService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TablesRestControllerTest {

    private static final UUID CLUB_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();

    private TableService tableService;
    private ClubService clubService;
    private AuthenticatedUserResolver authenticatedUserResolver;
    private TablesRestController controller;

    @Before
    public void setUp() {
        tableService = mock(TableService.class);
        clubService = mock(ClubService.class);
        authenticatedUserResolver = mock(AuthenticatedUserResolver.class);
        controller = new TablesRestController(
                tableService, new TableDtoAssembler(), clubService, authenticatedUserResolver);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUriBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/"));
        when(uriInfo.getAbsolutePathBuilder()).thenAnswer(invocation -> UriBuilder.fromUri("http://localhost/api/clubs/" + CLUB_ID + "/tables"));
        ReflectionTestUtils.setField(controller, "uriInfo", uriInfo);
    }

    @Test
    public void testListTables_InvalidClubId_ReturnsBadRequest() {
        Response response = controller.listTables("not-a-uuid", null, mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListTables_InvalidDate_ReturnsBadRequest() {
        Response response = controller.listTables(CLUB_ID.toString(), "no-es-una-fecha", mock(SecurityContext.class));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testListTables_NoDate_ReturnsEmbeddedCollectionFromClub() {
        when(tableService.findByClubId(CLUB_ID)).thenReturn(List.of(table(TABLE_ID)));

        Response response = controller.listTables(CLUB_ID.toString(), null, mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("1", response.getHeaderString("X-Total-Count"));
        @SuppressWarnings("unchecked")
        List<TableDto> tables = (List<TableDto>) response.getEntity();
        assertEquals(TABLE_ID, tables.get(0).getTableId());
        assertTrue(tables.get(0).getLinks().stream().anyMatch(link -> "self".equals(link.getRel())));
    }

    @Test
    public void testListTables_WithDate_UsesAvailabilitySearch() {
        when(tableService.findAvailableOnDate(any(), any(), any(), any()))
                .thenReturn(List.of(table(TABLE_ID), table(UUID.randomUUID())));

        Response response = controller.listTables(CLUB_ID.toString(), "2026-08-01", mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        // Si no hubiera usado findAvailableOnDate, el total vendría de
        // findByClubId (sin stubear), que devuelve una lista vacía por default.
        assertEquals("2", response.getHeaderString("X-Total-Count"));
    }

    @Test
    public void testCreateTable_Unauthenticated_ReturnsUnauthorized() {
        when(authenticatedUserResolver.resolve(any())).thenReturn(null);
        Response response = controller.createTable(CLUB_ID.toString(), validTableDto(), mock(SecurityContext.class));
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateTable_MissingFields_ReturnsBadRequest() {
        when(authenticatedUserResolver.resolve(any())).thenReturn(user(UUID.randomUUID()));
        TableDto body = new TableDto();
        body.setTableNumber(5);

        Response response = controller.createTable(CLUB_ID.toString(), body, mock(SecurityContext.class));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void testCreateTable_Success_ReturnsCreated() {
        when(authenticatedUserResolver.resolve(any())).thenReturn(user(UUID.randomUUID()));
        when(tableService.create(
                BigDecimal.valueOf(5), BigDecimal.valueOf(4), CLUB_ID, TableState.DISPONIBLE,
                BigDecimal.valueOf(1000), BigDecimal.ZERO
        )).thenReturn(table(TABLE_ID));

        Response response = controller.createTable(CLUB_ID.toString(), validTableDto(), mock(SecurityContext.class));

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertTrue(response.getLocation().toString().contains(TABLE_ID.toString()));
    }

    @Test
    public void testGetTable_NotFoundForClub_ReturnsNotFound() {
        when(tableService.findById(TABLE_ID)).thenReturn(Optional.empty());
        Response response = controller.getTable(CLUB_ID.toString(), TABLE_ID.toString(), mock(SecurityContext.class));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void testGetTable_Found_ReturnsTableDto() {
        when(tableService.findById(TABLE_ID)).thenReturn(Optional.of(table(TABLE_ID)));
        Response response = controller.getTable(CLUB_ID.toString(), TABLE_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getEntity() instanceof TableDto);
        assertEquals(TABLE_ID, ((TableDto) response.getEntity()).getTableId());
    }

    @Test
    public void testDeleteTable_Success_ReturnsNoContent() {
        when(authenticatedUserResolver.resolve(any())).thenReturn(user(UUID.randomUUID()));
        when(tableService.findById(TABLE_ID)).thenReturn(Optional.of(table(TABLE_ID)));
        when(tableService.delete(TABLE_ID)).thenReturn(true);

        Response response = controller.deleteTable(CLUB_ID.toString(), TABLE_ID.toString(), mock(SecurityContext.class));

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    private static TableDto validTableDto() {
        TableDto dto = new TableDto();
        dto.setTableNumber(5);
        dto.setCapacity(4);
        dto.setPrice(BigDecimal.valueOf(1000));
        dto.setDiscount(BigDecimal.ZERO);
        dto.setTableState(TableState.DISPONIBLE);
        return dto;
    }

    private static ClubTable table(UUID id) {
        return new ClubTable(id, CLUB_ID, 5, 4, TableState.DISPONIBLE, BigDecimal.valueOf(1000), BigDecimal.ZERO);
    }

    private static User user(UUID id) {
        User user = new User(
                id, "user-" + id, "Name", "Surname", id + "@example.com",
                "$2a$10$012345678901234567890uG0k34v3Ktb5sQ8kOLTNf5/4RswlD2oa",
                "1122334455", Role.USER, OffsetDateTime.now(), "es"
        );
        user.setVerified(true);
        return user;
    }
}
