package ar.edu.itba.paw.webapp.rest.clubs;

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

    @GET
    public Response getClub(){
        return Response.ok().build();
    }
//
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
//            all.removeIf(c -> c.clubName == null || !c.clubName.toLowerCase(Locale.ROOT).contains(n));
//        }
//        if (zone != null && !zone.isBlank()) {
//            all.removeIf(c -> c.zone == null || !c.zone.equalsIgnoreCase(zone));
//        }
//        if (genre != null && !genre.isBlank()) {
//            all.removeIf(c -> c.musicGenre == null || !c.musicGenre.equalsIgnoreCase(genre));
//        }
//        if (price != null && !price.isBlank()) {
//            all.removeIf(c -> c.priceRange == null || !c.priceRange.equalsIgnoreCase(price));
//        }
//        if (isBar != null) {
//            all.removeIf(c -> c.isBar == null || !c.isBar.equals(isBar));
//        }
//
//        // Paginación simple 1-based como en tu OpenAPI
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

//    @GET
//    @Path("/{clubId}")
//    public Response getClub(@PathParam("clubId") String clubId) {
//        UUID id;
//        try {
//            id = UUID.fromString(clubId);
//        } catch (IllegalArgumentException e) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity(Map.of("message", "clubId inválido"))
//                    .build();
//        }
//
//        ClubDto club = CLUBS.get(id);
//        if (club == null) {
//            return Response.status(Response.Status.NOT_FOUND)
//                    .entity(Map.of("message", "Club no encontrado"))
//                    .build();
//        }
//
//        return Response.ok(club).build();
//    }

    @POST
    public Response createClub(ClubCreateRequestDto body) {
        if (body == null ||
                isBlank(body.clubName) ||
                isBlank(body.location) ||
                isBlank(body.description) ||
                isBlank(body.zone) ||
                isBlank(body.musicGenre) ||
                isBlank(body.priceRange) ||
                body.isBar == null ||
                isBlank(body.cbu)
        ) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Faltan campos requeridos"))
                    .build();
        }

        UUID id = UUID.randomUUID();

        ClubDto created = new ClubDto();
        created.setClubId(id);
        created.setClubName(body.clubName);
        created.setDescription(body.description);
        created.setLocation(body.location);
        created.setZone(body.zone);
        created.setMusicGenre(body.musicGenre);
        created.setPriceRange(body.priceRange);
        created.setBar(body.isBar);
        created.setCbu(body.cbu);
        created.setRating(0.0F);
        created.setQuantityRatings(0);
        created.setCreatedAt(OffsetDateTime.parse(OffsetDateTime.now().toString()));
//        created.imageUrl = body.imageUrl;

        CLUBS.put(id, created);

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getClubId().toString())
                .build();
        return Response.created(uri).entity(created).build();
    }

//    @PUT
//    @Path("/{clubId}")
//    public Response updateClub(@PathParam("clubId") String clubId, ClubCreateRequestDto body) {
//        UUID id;
//        try {
//            id = UUID.fromString(clubId);
//        } catch (IllegalArgumentException e) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity(Map.of("message", "clubId inválido"))
//                    .build();
//        }
//
//        ClubDto existing = CLUBS.get(id);
//        if (existing == null) {
//            return Response.status(Response.Status.NOT_FOUND)
//                    .entity(Map.of("message", "Club no encontrado"))
//                    .build();
//        }
//
//        if (body == null) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity(Map.of("message", "Body requerido"))
//                    .build();
//        }
//
//        // Update completo (como en tu OpenAPI usás ClubCreateRequest para PUT)
//        if (!isBlank(body.clubName)) existing.clubName = body.clubName;
//        if (!isBlank(body.description)) existing.description = body.description;
//        if (!isBlank(body.location)) existing.location = body.location;
//        if (!isBlank(body.zone)) existing.zone = body.zone;
//        if (!isBlank(body.musicGenre)) existing.musicGenre = body.musicGenre;
//        if (!isBlank(body.priceRange)) existing.priceRange = body.priceRange;
//        if (body.isBar != null) existing.isBar = body.isBar;
//        if (!isBlank(body.cbu)) existing.cbu = body.cbu;
//        existing.imageUrl = body.imageUrl;
//
//        CLUBS.put(id, existing);
//        return Response.ok(existing).build();
//    }

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

    // DTOs mínimos para testear. Después se reemplazan por tus modelos reales.
    public static class ClubCreateRequestDto {
        public String clubName;
        public String location;
        public String priceRange;
        public String description;
        public String musicGenre;
        public String zone;
        public Boolean isBar;
        public String cbu;
//        public String imageUrl;
    }

}