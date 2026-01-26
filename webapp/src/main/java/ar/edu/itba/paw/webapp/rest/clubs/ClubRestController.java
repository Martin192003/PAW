package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.dto.ClubCreateRequestDto;
import ar.edu.itba.paw.model.dto.ClubDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Context;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON) // TODO: Ojo con el 415 acá
public class ClubRestController {

    private static final Map<UUID, ClubDto> CLUBS = new ConcurrentHashMap<>();

    @Context
    private UriInfo uriInfo;


//    @GET
//    public Response listClubs(
//            @QueryParam("name") String name,
//            @QueryParam("zone") String zone,
//            @QueryParam("genre") String genre,
//            @QueryParam("price") String price,
//            @QueryParam("isBar") Boolean isBar,
//            @QueryParam("page") @DefaultValue("1") int page,
//            @QueryParam("size") @DefaultValue("10") int size
//    ) {
//        List<ClubDto> all = new ArrayList<>(CLUBS.values());
//
//        if (name != null && !name.isBlank()) {
//            String n = name.toLowerCase(Locale.ROOT);
//            all.removeIf(c -> c.getClubName() == null || !c.getClubName().toLowerCase(Locale.ROOT).contains(n));
//        }
//        if (zone != null && !zone.isBlank()) {
//            all.removeIf(c -> c.getZone() == null || !c.getZone().equalsIgnoreCase(zone));
//        }
//        if (genre != null && !genre.isBlank()) {
//            all.removeIf(c -> c.getMusicGenre() == null || !c.getMusicGenre().equalsIgnoreCase(genre));
//        }
//        if (price != null && !price.isBlank()) {
//            all.removeIf(c -> c.getPriceRange() == null || !c.getPriceRange().equalsIgnoreCase(price));
//        }
//        if (isBar != null) {
//            all.removeIf(c -> !c.isBar());
//        }
//
//        int totalResults = all.size();
//        int totalPages = (int) Math.ceil(totalResults / (double) size);
//        int from = Math.max(0, (page - 1) * size);
//        int to = Math.min(totalResults, from + size);
//
//        List<ClubDto> pageItems = from >= totalResults ? Collections.emptyList() : all.subList(from, to);
//
//        return Response.ok(pageItems)
//                .header("X-Total-Results", String.valueOf(totalResults))
//                .header("X-Total-Pages", String.valueOf(Math.max(totalPages, 1)))
//                .header("X-Page", String.valueOf(page))
//                .header("X-Size", String.valueOf(size))
//                .build();
//    }

    @GET
    @Path("/{clubId}")
    public Response getClub(@PathParam("clubId") String clubId) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        ClubDto club = CLUBS.get(id);
        if (club == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        return Response.ok(club).build();
    }

    @POST
    public Response createClub(ClubCreateRequestDto body) {
        if (body == null ||
                isBlank(body.getClubName()) ||
                isBlank(body.getLocation()) ||
                isBlank(body.getDescription()) ||
                isBlank(body.getZone()) ||
                isBlank(body.getMusicGenre()) ||
                isBlank(body.getPriceRange()) ||
                isBlank(body.getCbu())
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Faltan campos requeridos"))
                    .build();
        }

        UUID id = UUID.randomUUID();

        ClubDto created = new ClubDto();
        created.setClubId(id);
        created.setClubName(body.getClubName());
        created.setDescription(body.getDescription());
        created.setLocation(body.getLocation());
        created.setZone(body.getZone());
        created.setMusicGenre(body.getMusicGenre());
        created.setPriceRange(body.getPriceRange());
        created.setBar(body.isBar());
        created.setCbu(body.getCbu());
        created.setRating(0.0F);
        created.setQuantityRatings(0);
        created.setCreatedAt(OffsetDateTime.now());
        //        created.imageUrl = body.imageUrl;

        CLUBS.put(id, created);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getClubId().toString())
                .build();
        return Response.created(uri).entity(created).build();
    }

    @PUT
    @Path("/{clubId}")
    public Response updateClub(@PathParam("clubId") String clubId, ClubCreateRequestDto body) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        ClubDto existing = CLUBS.get(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Body requerido"))
                    .build();
        }

        // Update completo (como en tu OpenAPI usás ClubCreateRequest para PUT)
        if (!isBlank(body.getClubName())) existing.setClubName(body.getClubName());
        if (!isBlank(body.getDescription())) existing.setDescription(body.getDescription());
        if (!isBlank(body.getLocation())) existing.setLocation(body.getLocation());
        if (!isBlank(body.getZone())) existing.setZone(body.getZone());
        if (!isBlank(body.getMusicGenre())) existing.setMusicGenre(body.getMusicGenre());
        if (!isBlank(body.getPriceRange())) existing.setPriceRange(body.getPriceRange());
        if (body.isBar()) existing.setBar(body.isBar());
        if (!isBlank(body.getCbu())) existing.setCbu(body.getCbu());
//        existing.imageUrl = body.imageUrl;

        CLUBS.put(id, existing);
        return Response.ok(existing).build();
    }

    @DELETE
    @Path("/{clubId}")
    public Response deleteClub(@PathParam("clubId") String clubId) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        ClubDto removed = CLUBS.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        return Response.noContent().build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }



}