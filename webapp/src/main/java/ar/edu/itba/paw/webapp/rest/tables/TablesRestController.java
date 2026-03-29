package ar.edu.itba.paw.webapp.rest.tables;

import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.dto.TableCreateRequestDto;
import ar.edu.itba.paw.model.dto.TableUpdateRequestDto;
import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.TableService;
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

@Path("/clubs/{clubId}/tables")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TablesRestController {

    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Clock CLOCK = Clock.system(BUENOS_AIRES_ZONE);

    private final TableService tableService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public TablesRestController(final TableService tableService) {
        this.tableService = tableService;
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
            final List<ClubTable> tables;
            if (date != null && !date.isBlank()) {
                LocalDate selectedDate;
                try {
                    selectedDate = LocalDate.parse(date);
                } catch (Exception e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("message", "date inválida, formato esperado yyyy-MM-dd"))
                            .build();
                }
                tables = tableService.findAvailableOnDate(clubUuid, selectedDate, BUENOS_AIRES_ZONE, CLOCK);
            } else {
                tables = tableService.findByClubId(clubUuid);
            }

            return Response.ok(new GenericEntity<>(tables) {}).build();
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
    public Response createTable(@PathParam("clubId") String clubId, TableCreateRequestDto body) {
        UUID clubUuid;
        try {
            clubUuid = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
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
        return Response.created(uri).entity(created).build();
    }

    @PUT
    @Path("/{tableId}")
    public Response updateTable(@PathParam("clubId") String clubId,
                                @PathParam("tableId") String tableId,
                                TableUpdateRequestDto body) {
        UUID clubUuid, tableUuid;
        try {
            clubUuid = UUID.fromString(clubId);
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId o tableId inválido"))
                    .build();
        }

        //TODO: no se si hacer un metodo generico pero son solo 2 veces (?)
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

        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{tableId}")
    public Response deleteTable(@PathParam("clubId") String clubId,
                                @PathParam("tableId") String tableId) {
        UUID tableUuid;
        try {
            tableUuid = UUID.fromString(tableId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "tableId inválido"))
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
}