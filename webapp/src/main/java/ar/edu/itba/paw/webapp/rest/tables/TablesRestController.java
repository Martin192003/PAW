package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.TableCreateRequestDto;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.model.dto.TableUpdateRequestDto;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.TableService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/clubs/{clubId}/tables")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TablesRestController {

    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock CLOCK = Clock.system(BUENOS_AIRES_ZONE);

    private final TableService tableService;
    private final TableDtoAssembler tableDtoAssembler;
    private final ClubService clubService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public TablesRestController(final TableService tableService,
                                final TableDtoAssembler tableDtoAssembler,
                                final ClubService clubService,
                                final AuthenticatedUserResolver authenticatedUserResolver) {
        this.tableService = tableService;
        this.tableDtoAssembler = tableDtoAssembler;
        this.clubService = clubService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GET
    public Response listTables(@PathParam("clubId") String clubId,
                               @QueryParam("date") String date) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        try {
            final List<TableDto> tables;
            if (date != null && !date.isBlank()) {
                LocalDate selectedDate;
                try {
                    selectedDate = LocalDate.parse(date);
                } catch (Exception e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("message", "date inválida, formato esperado yyyy-MM-dd"))
                            .build();
                }
                tables = tableService.findAvailableOnDate(clubUuid, selectedDate, BUENOS_AIRES_ZONE, CLOCK).stream()
                        .map(table -> tableDtoAssembler.toDto(uriInfo, clubUuid, table))
                        .collect(Collectors.toList());
            } else {
                tables = tableService.findByClubId(clubUuid).stream()
                        .map(table -> tableDtoAssembler.toDto(uriInfo, clubUuid, table))
                        .collect(Collectors.toList());
            }

            return Response.ok(new GenericEntity<List<TableDto>>(tables) {}).build();
        } catch (ValidationException | InvalidDateRangeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }
    }

    @POST
    public Response createTable(@PathParam("clubId") String clubId,
                                TableCreateRequestDto body,
                                @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (body == null ||
                body.getTableNumber() == null ||
                body.getCapacity() == null ||
                body.getPrice() == null ||
                body.getDiscount() == null ||
                body.getTableState() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Missing required fields"))
                    .build();
        }

        ClubTable created = tableService.create(
                BigDecimal.valueOf(body.getTableNumber()),
                BigDecimal.valueOf(body.getCapacity()),
                clubUuid,
                body.getTableState(),
                body.getPrice(),
                body.getDiscount()
        );
        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getTableId().toString())
                .build();
        return Response.created(uri)
                .entity(tableDtoAssembler.toDto(uriInfo, clubUuid, created))
                .build();
    }

    @GET
    @Path("/{tableId}")
    public Response getTable(@PathParam("clubId") String clubId,
                             @PathParam("tableId") String tableId) {
        UUID clubUuid;
        UUID tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o tableId invalido"))
                    .build();
        }

        return tableService.findById(tableUuid)
                .filter(table -> clubUuid.equals(table.getClubId()))
                .map(table -> Response.ok(tableDtoAssembler.toDto(uriInfo, clubUuid, table)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "Mesa no encontrada"))
                        .build());
    }

    @PUT
    @Path("/{tableId}")
    public Response updateTable(@PathParam("clubId") String clubId,
                                @PathParam("tableId") String tableId,
                                TableUpdateRequestDto body,
                                @Context SecurityContext securityContext) {
        UUID clubUuid, tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o tableId inválido"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!tableBelongsToClub(clubUuid, tableUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa o club no encontrado"))
                    .build();
        }

        if (body == null ||
                body.getTableNumber() == null ||
                body.getCapacity() == null ||
                body.getPrice() == null ||
                body.getDiscount() == null ||
                body.getTableState() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Missing required fields"))
                    .build();
        }

        ClubTable updated = tableService.update(
                tableUuid,
                clubUuid,
                BigDecimal.valueOf(body.getTableNumber()),
                BigDecimal.valueOf(body.getCapacity()),
                body.getTableState(),
                body.getPrice(),
                body.getDiscount()
        );
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa o club no encontrado"))
                    .build();
        }

        TableDto responseBody = tableDtoAssembler.toDto(uriInfo, clubUuid, updated);
        return Response.ok(responseBody).build();
    }

    @PUT
    @Path("/{tableId}/state")
    public Response updateTableState(@PathParam("clubId") String clubId,
                                     @PathParam("tableId") String tableId,
                                     TableUpdateRequestDto body,
                                     @Context SecurityContext securityContext) {
        UUID clubUuid, tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o tableId inválido"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (body == null || body.getTableState() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Missing required fields"))
                    .build();
        }

        ClubTable current = tableService.findByClubId(clubUuid).stream()
                .filter(t -> t.getTableId().equals(tableUuid))
                .findFirst()
                .orElse(null);

        if (current == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa o club no encontrado"))
                    .build();
        }

        ClubTable updated = tableService.update(
                tableUuid,
                clubUuid,
                BigDecimal.valueOf(current.getTableNumber()),
                BigDecimal.valueOf(current.getCapacity()),
                body.getTableState(),
                current.getPrice(),
                current.getDiscount()
        );

        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa o club no encontrado"))
                    .build();
        }

        TableDto responseBody = tableDtoAssembler.toDto(uriInfo, clubUuid, updated);
        return Response.ok(responseBody).build();
    }

    @DELETE
    @Path("/{tableId}")
    public Response deleteTable(@PathParam("clubId") String clubId,
                                @PathParam("tableId") String tableId,
                                @Context SecurityContext securityContext) {
        UUID clubUuid, tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o tableId inválido"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!tableBelongsToClub(clubUuid, tableUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa no encontrada"))
                    .build();
        }

        boolean removed = tableService.delete(tableUuid);
        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Mesa no encontrada"))
                    .build();
        }

        return Response.noContent().build();
    }

    private boolean tableBelongsToClub(UUID clubId, UUID tableId) {
        return tableService.findById(tableId)
                .filter(table -> clubId.equals(table.getClubId()))
                .isPresent();
    }
}
