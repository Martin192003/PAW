package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.TableService;
import ar.edu.itba.paw.webapp.rest.ApiMediaTypes;
import ar.edu.itba.paw.webapp.rest.DtoValidation;
import ar.edu.itba.paw.webapp.rest.CollectionLinks;
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
import java.util.UUID;

@Path("/clubs/{clubId}/tables")
@Produces({ApiMediaTypes.TABLE, ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
@Consumes({ApiMediaTypes.TABLE, ApiMediaTypes.APPLICATION_JSON})
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
    @Produces({ApiMediaTypes.LINK_COLLECTION, ApiMediaTypes.APPLICATION_JSON})
    public Response listTables(@PathParam("clubId") String clubId,
                               @QueryParam("date") String date,
                               @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club.id.invalid"))
                    .build();
        }

        try {
            final List<ClubTable> tables;
            if (date != null && !date.isBlank()) {
                LocalDate selectedDate;
                try {
                    selectedDate = LocalDate.parse(date);
                } catch (Exception e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ErrorDto("date.invalid"))
                            .build();
                }
                tables = tableService.findAvailableOnDate(clubUuid, selectedDate, BUENOS_AIRES_ZONE, CLOCK);
            } else {
                tables = tableService.findByClubId(clubUuid);
            }

            boolean canManage = canManageClub(clubUuid, securityContext);
            List<LinkDto> body = CollectionLinks.items(tables, table -> uriInfo.getBaseUriBuilder()
                    .path("clubs/{clubId}/tables/{tableId}")
                    .resolveTemplate("clubId", clubUuid)
                    .resolveTemplate("tableId", table.getTableId())
                    .build());

            Response.ResponseBuilder response = Response.ok(new GenericEntity<List<LinkDto>>(body) {})
                    .header("X-Total-Count", body.size());
            applyCollectionCacheHeaders(response, canManage);
            return response.build();
        } catch (ValidationException | InvalidDateRangeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto(e.getMessage()))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("club.notfound"))
                    .build();
        }
    }

    @POST
    public Response createTable(@PathParam("clubId") String clubId,
                                TableDto body,
                                @Context SecurityContext securityContext) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!DtoValidation.isValid(body, ValidationGroups.Create.class)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("fields.required"))
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
                .entity(tableDtoAssembler.toDto(uriInfo, clubUuid, created, true))
                .build();
    }

    @GET
    @Path("/{tableId}")
    public Response getTable(@PathParam("clubId") String clubId,
                             @PathParam("tableId") String tableId,
                             @Context SecurityContext securityContext) {
        UUID clubUuid;
        UUID tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club_or_table.id.invalid"))
                    .build();
        }

        boolean canManage = canManageClub(clubUuid, securityContext);
        return tableService.findById(tableUuid)
                .filter(table -> clubUuid.equals(table.getClubId()))
                .map(table -> Response.ok(tableDtoAssembler.toDto(uriInfo, clubUuid, table, canManage)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorDto("table.notfound"))
                        .build());
    }

    @PUT
    @Path("/{tableId}")
    public Response updateTable(@PathParam("clubId") String clubId,
                                @PathParam("tableId") String tableId,
                                TableDto body,
                                @Context SecurityContext securityContext) {
        UUID clubUuid, tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("club_or_table.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!tableBelongsToClub(clubUuid, tableUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("table_or_club.notfound"))
                    .build();
        }

        if (!DtoValidation.isValid(body, ValidationGroups.Update.class)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("fields.required"))
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
                    .entity(new ErrorDto("table_or_club.notfound"))
                    .build();
        }

        TableDto responseBody = tableDtoAssembler.toDto(uriInfo, clubUuid, updated, true);
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
                    .entity(new ErrorDto("club_or_table.id.invalid"))
                    .build();
        }

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubUuid, user.getId());

        if (!tableBelongsToClub(clubUuid, tableUuid)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("table.notfound"))
                    .build();
        }

        boolean removed = tableService.delete(tableUuid);
        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("table.notfound"))
                    .build();
        }

        return Response.noContent().build();
    }

    private boolean tableBelongsToClub(UUID clubId, UUID tableId) {
        return tableService.findById(tableId)
                .filter(table -> clubId.equals(table.getClubId()))
                .isPresent();
    }

    private boolean canManageClub(UUID clubId, SecurityContext securityContext) {
        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return false;
        }
        try {
            clubService.assertAdmin(clubId, user.getId());
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void applyCollectionCacheHeaders(Response.ResponseBuilder response, boolean canManage) {
        if (canManage) {
            response.header("Cache-Control", "private, no-store")
                    .header("Pragma", "no-cache")
                    .header("Vary", "Authorization");
            return;
        }
        response.header("Cache-Control", "no-cache");
    }
}

