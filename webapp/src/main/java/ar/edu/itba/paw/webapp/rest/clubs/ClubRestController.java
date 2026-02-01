package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.dto.ClubCreateRequestDto;
import ar.edu.itba.paw.model.dto.ClubDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ar.edu.itba.paw.webapp.rest.clubs.ClubStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON) // TODO: Ojo con el 415 acá
public class ClubRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ClubService clubService;
    private final TableService tableService;


    @Context
    private UriInfo uriInfo;

    @Autowired
    public ClubRestController(final ClubService clubService, final TableService tableService) {
        this.clubService = clubService;
        this.tableService = tableService;
    }

    @GET
    public Response listClubs(@QueryParam("name") final String nameLike,
                              @QueryParam("zone") final Zone zone,
                              @QueryParam("genre") final String musicGenre,
                              @QueryParam("price") final String priceRange,
                              @QueryParam("isBar") final Boolean isBar,
                              @QueryParam("availableToday") final Boolean availableToday,
                              @QueryParam("sort") final String sort,
                              @QueryParam("page") @DefaultValue("1") int page,
                              @QueryParam("size") @DefaultValue("10") int size) {

        page = (page < 1) ? DEFAULT_PAGE : page;
        size = (size < 1) ? DEFAULT_SIZE : size;

        final String normalizedPriceRange =
                (priceRange == null || priceRange.isBlank()) ? null : priceRange.toUpperCase();

        final boolean availabilityMode = isAvailabilityMode(availableToday, sort);
        final LocalDate selectedDay = resolveSelectedDay(availabilityMode, sort);
        final boolean invalidDay = availabilityMode && !tableService.isAllowedDay(selectedDay);

        int totalClubs = 0;
        List<Club> clubs = Collections.emptyList();
        final int offset = (page - 1) * size;

        if (availabilityMode) {
            if (!invalidDay) {
                totalClubs = clubService.countSearchAvailable(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay
                );
                clubs = clubService.searchAvailablePaginated(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset
                );
            }
        } else {
            totalClubs = clubService.countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
            clubs = clubService.searchPaginated(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset
            );
        }

        final int totalPages = (size == 0) ? 0 : (int) Math.ceil((double) totalClubs / size);

        final GenericEntity<List<Club>> entity = new GenericEntity<>(clubs) {};
        final Response.ResponseBuilder rb = Response.ok(entity);

        rb.header("X-Total-Results", totalClubs);
        rb.header("X-Total-Pages", totalPages);
        rb.header("X-Page", page);
        rb.header("X-Size", size);
        rb.header("X-Available-Today", availabilityMode);
        rb.header("X-Invalid-Day", invalidDay);

        if (availabilityMode) {
            rb.header("X-Selected-Availability-Date", selectedDay != null ? selectedDay.toString() : "");
        }

        return rb.build();
    }

    private boolean isAvailabilityMode(final Boolean availableToday, final String sort) {
        return (availableToday != null && availableToday)
                || (sort != null && sort.startsWith("availability_"));
    }

    private LocalDate resolveSelectedDay(final boolean availabilityMode, final String sort) {
        if (!availabilityMode) {
            return null;
        }
        final Integer dayOffset = extractDayOffsetFromSort(sort);
        return (dayOffset != null)
                ? LocalDate.now(BUENOS_AIRES_ZONE).plusDays(dayOffset)
                : LocalDate.now(BUENOS_AIRES_ZONE);
    }

    private Integer extractDayOffsetFromSort(final String sort) {
        if (sort == null) {
            return null;
        }
        final Pattern pattern = Pattern.compile("availability_d([0-2])_(?:asc|desc)");
        final Matcher matcher = pattern.matcher(sort);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }



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

        ClubDto club = ClubStore.CLUBS.get(id);
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

        ClubStore.CLUBS.put(id, created);

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

        ClubDto existing = ClubStore.CLUBS.get(id);
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

        ClubStore.CLUBS.put(id, existing);
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

        ClubDto removed = ClubStore.CLUBS.remove(id);
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