package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.dto.*;
import ar.edu.itba.paw.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

@Component
@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
public class ClubRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ClubService clubService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubLayoutService clubLayoutService;
    private final PhotoService photoService;


    @Context
    private UriInfo uriInfo;

    @Autowired
    public ClubRestController(final ClubService clubService, final TableService tableService, UserService userService, ClubLayoutService clubLayoutService, PhotoService photoService) {
        this.clubService = clubService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubLayoutService = clubLayoutService;
        this.photoService = photoService;
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

        List<ClubDto> dtos = clubs.stream()
                .map(c -> toDtoWithLinks(c))
                .toList();


        PagedResponseDto response = new PagedResponseDto(
                dtos,
                page,
                size,
                totalClubs,
                totalPages,
                availabilityMode,
                invalidDay,
                selectedDay != null ? selectedDay.toString() : null
        );

        return Response.ok(response)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .build();
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

        Club club = clubService.getClubById(id).orElse(null);
        if (club == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        return Response.ok(toDtoWithLinks(club)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClub(ClubCreateRequestDto body,
                               @Context SecurityContext securityContext) {

        if (body == null ||
                isBlank(body.getClubName()) ||
                isBlank(body.getLocation()) ||
                isBlank(body.getDescription()) ||
                isBlank(body.getZone()) ||
                isBlank(body.getMusicGenre()) ||
                isBlank(body.getPriceRange()) ||
                isBlank(body.getCbu())) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Faltan campos requeridos"))
                    .build();
        }

        String email = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(email);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Club created = clubService.create(
                body.getClubName(),
                body.getDescription(),
                body.getLocation(),
                Price.valueOf(body.getPriceRange()),
                user,
                MusicalGenre.valueOf(body.getMusicGenre()),
                Zone.valueOf(body.getZone()),
                body.isBar(),
                body.getCbu(),
                null
        );

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getClubId().toString())
                .build();

        return Response.created(uri)
                .entity(toDtoWithLinks(created))
                .build();
    }

    @PUT
    @Path("/{clubId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClub(@PathParam("clubId") String clubId, ClubCreateRequestDto body, @Context SecurityContext securityContext) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        Optional<Club> existingOpt = clubService.getClubById(id);
        if (existingOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Body requerido"))
                    .build();
        }

        Club existing = existingOpt.get();
        String email = securityContext.getUserPrincipal().getName();
        User user = userService.findByEmail(email);

        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Price price = body.getPriceRange() != null
                ? Price.valueOf(body.getPriceRange())
                : existing.getPriceRange();

        MusicalGenre genre = body.getMusicGenre() != null
                ? MusicalGenre.valueOf(body.getMusicGenre())
                : existing.getMusicGenre();

        Zone zone = body.getZone() != null
                ? Zone.valueOf(body.getZone())
                : existing.getZone();

        String name = body.getClubName() != null ? body.getClubName() : existing.getClubName();
        String description = body.getDescription() != null ? body.getDescription() : existing.getDescription();
        String location = body.getLocation() != null ? body.getLocation() : existing.getLocation();
        Boolean isBar = body.isBar();
        String cbu = body.getCbu() != null ? body.getCbu() : existing.getCbu();

        Club updated = clubService.update(
                id,
                name,
                location,
                existing.getRating(),
                existing.getQuantityRatings(),
                price,
                description,
                existing.getContactInfo() != null
                        ? existing.getContactInfo().getId()
                        : null,
                genre,
                zone,
                isBar,
                cbu
        );

        return Response.ok(toDtoWithLinks(updated)).build();
    }

    @GET
    @Path("/{clubId}/layout")
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
    @Path("/{clubId}/limits")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLimits(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubService.getLimits(id)
                .map(limits -> Response.ok(limits).build())
                .orElse(notFound("Club no encontrado"));
    }

    @GET
    @Path("/{clubId}/cover")
    public Response getCover(@PathParam("clubId") String clubId, @Context Request request) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        if (clubService.getClubById(id).isEmpty())
            return notFound("Club no encontrado");

        return photoService.getCoverPhoto(id)
                .map(photo -> buildImageResponse(photo, request))
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }


    @GET
    @Path("/{clubId}/layout/image")
    public Response getLayoutImage(@PathParam("clubId") String clubId, @Context Request request) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubLayoutService.getLayoutByClub(id)
                .map(layout -> {

                    EntityTag etag = new EntityTag(
                            Integer.toHexString(
                                    Objects.hash(layout.getClubId(), layout.getSizeBytes())
                            )
                    );

                    Response.ResponseBuilder preconditions =
                            request.evaluatePreconditions(etag);

                    CacheControl cacheControl = new CacheControl();
                    cacheControl.setMaxAge(31536000);
                    cacheControl.setPrivate(false);

                    if (preconditions != null) {
                        return preconditions
                                .cacheControl(cacheControl)
                                .tag(etag)
                                .build();
                    }

                    return Response.ok(layout.getData(), layout.getContentType())
                            .cacheControl(cacheControl)
                            .tag(etag)
                            .header("Content-Length", layout.getSizeBytes())
                            .build();
                })
                .orElse(Response.noContent().build());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ClubDto toDtoWithLinks(Club club) {

        ClubDto dto = new ClubDto();

        dto.setClubId(club.getClubId());
        dto.setClubName(club.getClubName());
        dto.setDescription(club.getDescription());
        dto.setLocation(club.getLocation());
        dto.setZone(club.getZone().name());
        dto.setMusicGenre(club.getMusicGenre().toString());
        dto.setPriceRange(club.getPriceRange().toString());
        dto.setBar(club.getIsBar());
        dto.setRating(
                club.getRating() != null
                        ? club.getRating().floatValue()
                        : 0f
        );
        dto.setQuantityRatings(
                club.getQuantityRatings() != null
                        ? club.getQuantityRatings()
                        : 0
        );
        dto.setCreatedAt(club.getCreatedAt());


        ClubLinksDto links = new ClubLinksDto();

        links.setSelf(
                uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(club.getClubId().toString())
                        .build()
                        .toString()
        );

        links.setCover(
                uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(club.getClubId().toString())
                        .path("cover")
                        .build()
                        .toString()
        );

        links.setLayout(
                uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(club.getClubId().toString())
                        .path("layout")
                        .build()
                        .toString()
        );

        links.setLimits(
                uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(club.getClubId().toString())
                        .path("limits")
                        .build()
                        .toString()
        );

        dto.setLinks(links);

        return dto;
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

    private Response buildImageResponse(Photo photo, Request request) {

        EntityTag etag = new EntityTag(
                Integer.toHexString(
                        Objects.hash(photo.getPhotoId(), photo.getSizeBytes())
                )
        );

        Response.ResponseBuilder preconditions =
                request.evaluatePreconditions(etag);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(31536000);
        cacheControl.setPrivate(false);

        if (preconditions != null) {
            return preconditions
                    .cacheControl(cacheControl)
                    .tag(etag)
                    .build();
        }

        return Response.ok(photo.getData(), photo.getContentType())
                .cacheControl(cacheControl)
                .tag(etag)
                .header("Content-Length", photo.getSizeBytes())
                .build();
    }
}


