package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.dto.ClubDto;
import ar.edu.itba.paw.model.dto.CoverDto;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.model.dto.LayoutDto;
import ar.edu.itba.paw.service.ClubLayoutService;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;

@Component
@Path("/clubs/{clubId}")
public class ClubSubresourcesRestController {

    private final ClubService clubService;
    private final PhotoService photoService;
    private final ClubLayoutService clubLayoutService;


    public ClubSubresourcesRestController(
            ClubService clubService,
            PhotoService photoService,
            ClubLayoutService clubLayoutService
    ) {
        this.clubService = clubService;
        this.photoService = photoService;
        this.clubLayoutService = clubLayoutService;
    }

    @GET
    @Path("/cover")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCover(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        if (clubService.getClubById(id).isEmpty())
            return notFound("Club no encontrado");

        return photoService.getCoverUrl(id)
                .map(url -> Response.ok(
                        new CoverDto(id.toString(), url)
                ).build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("/layout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLayout(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        if (clubService.getClubById(id).isEmpty())
            return notFound("Club no encontrado");

        return clubLayoutService.getLayoutByClub(id)
                .map(layout -> Response.ok(
                        new LayoutDto(
                                id.toString(),
                                layout.getUrl()
                        )
                ).build())
                .orElse(Response.noContent().build());
    }


    @GET
    @Path("/layout/image")
    public Response getLayoutImage(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubLayoutService.getLayoutByClub(id)
                .map(layout -> Response.ok(
                                layout.getData(),
                                layout.getContentType()
                        )
                        .header("Content-Length", layout.getSizeBytes())
                        .build())
                .orElse(Response.noContent().build());
    }

    @GET
    @Path("/limits")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLimits(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubService.getLimits(id)
                .map(limits -> Response.ok(limits).build())
                .orElse(notFound("Club no encontrado"));
    }

    private static UUID parseUuidOr400(final String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Response badRequest(final String msg) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto(msg))
                .build();
    }

    private static Response notFound(final String msg) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorDto(msg))
                .build();
    }
}
