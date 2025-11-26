package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.service.ClubLayoutService;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.service.MenuService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.ProductService;
import ar.edu.itba.paw.service.ReviewService;
import ar.edu.itba.paw.service.TableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Path("/clubs")
@Component
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClubRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    private ClubService clubService;

    @Autowired
    private EventService eventService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private TableService tableService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private ClubLayoutService clubLayoutService;

    @Context
    private UriInfo uriInfo;

    @GET
    public Response listClubs(@QueryParam("name") String nameLike,
                              @QueryParam("zone") Zone zone,
                              @QueryParam("genre") String musicGenre,
                              @QueryParam("price") String priceRange,
                              @QueryParam("isBar") Boolean isBar,
                              @QueryParam("availableToday") Boolean availableToday,
                              @QueryParam("sort") String sort,
                              @QueryParam("page") @DefaultValue("1") int page,
                              @QueryParam("size") @DefaultValue("10") int size) {

        page = normalizePage(page);
        size = normalizeSize(size);
        final String normalizedPriceRange = normalizePriceRange(priceRange);

        final boolean availabilityMode = isAvailabilityMode(availableToday, sort);
        final LocalDate selectedDay = resolveSelectedDay(availabilityMode, sort);
        final boolean invalidDay = availabilityMode && !tableService.isAllowedDay(selectedDay);

        int totalClubs = 0;
        List<Club> clubs = Collections.emptyList();

        if (availabilityMode) {
            if (!invalidDay) {
                totalClubs = clubService.countSearchAvailable(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay
                );
                final int offset = clubService.offsetForPage(
                        page, size, nameLike, zone, musicGenre, normalizedPriceRange, isBar
                );
                clubs = clubService.searchAvailablePaginated(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset
                );
            }
        } else {
            totalClubs = clubService.countSearch(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar
            );
            final int offset = clubService.offsetForPage(
                    page, size, nameLike, zone, musicGenre, normalizedPriceRange, isBar
            );
            clubs = clubService.searchPaginated(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset
            );
        }

        final int totalPages = clubService.pagesForCount(
                size, nameLike, zone, musicGenre, normalizedPriceRange, isBar
        );

        final String selectedDayStr = (selectedDay != null) ? selectedDay.toString() : null;

        GenericEntity<List<Club>> entity = new GenericEntity<>(clubs) {};
        Response.ResponseBuilder rb = Response.ok(entity);
        rb.header("X-Total-Results", Integer.toString(totalClubs));
        rb.header("X-Total-Pages", Integer.toString(totalPages));
        rb.header("X-Page", Integer.toString(page));
        rb.header("X-Size", Integer.toString(size));
        rb.header("X-Available-Today", Boolean.toString(availabilityMode));
        rb.header("X-Invalid-Day", Boolean.toString(invalidDay));
        if (selectedDayStr != null) {
            rb.header("X-Selected-Availability-Date", selectedDayStr);
        }

        return rb.build();
    }

    @POST
    public Response createClub(final Club club) {
        final Club created = clubService.create(
                club.getClubName(),
                club.getDescription(),
                club.getLocation(),
                club.getPriceRange(),
                club.getContactInfo(),
                club.getMusicGenre(),
                club.getZone(),
                club.getIsBar(),
                club.getCbu(),
                null
        );

        final URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getClubId().toString())
                .build();

        return Response.created(uri).entity(created).build();
    }


    @GET
    @Path("/{clubId}")
    public Response getClub(@PathParam("clubId") UUID clubId) {
        return clubService.getClubById(clubId)
                .map(club -> Response.ok(club).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{clubId}")
    public Response updateClub(@PathParam("clubId") UUID clubId,
                               final Club changes) {

        final Club existing = clubService.getClubByIdOrThrow(clubId);

        existing.setClubName(changes.getClubName());
        existing.setLocation(changes.getLocation());
        existing.setZone(changes.getZone());
        existing.setMusicGenre(changes.getMusicGenre());
        existing.setPriceRange(changes.getPriceRange());
        existing.setDescription(changes.getDescription());
        existing.setCbu(changes.getCbu());

        clubService.updateClub(existing);
        return Response.ok(existing).build();
    }

    @GET
    @Path("/{clubId}/cover")
    public Response getClubCover(@PathParam("clubId") UUID clubId) {
        final Optional<String> maybeUrl = photoService.getCoverUrl(clubId);
        if (maybeUrl.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Map<String, String> payload = Collections.singletonMap("url", maybeUrl.get());
        return Response.ok(payload).build();
    }


    @GET
    @Path("/{clubId}/events")
    public Response listEventsByClub(@PathParam("clubId") UUID clubId,
                                     @QueryParam("filter") @DefaultValue("all") String filter,
                                     @QueryParam("page") @DefaultValue("1") int page,
                                     @QueryParam("size") @DefaultValue("10") int size) {

        page = normalizePage(page);
        size = normalizeSize(size);
        int pageIndex = page - 1;

        final List<Event> events;
        final int totalPages = switch (filter) {
            case "past" -> {
                events = eventService.findPastByClub(clubId, pageIndex, size);
                yield eventService.totalEventPagesPast(clubId, size);
            }
            case "future" -> {
                events = eventService.findUpcomingByClub(clubId, pageIndex, size);
                yield eventService.totalEventPagesUpcoming(clubId, size);
            }
            default -> {
                events = eventService.listByClub(clubId, pageIndex, size);
                yield eventService.totalEventPages(clubId, size);
            }
        };

        Map<String, Object> payload = new HashMap<>();
        payload.put("items", events);
        payload.put("page", page);
        payload.put("size", size);
        payload.put("totalPages", totalPages);
        payload.put("filter", filter);

        return Response.ok(payload).build();
    }


    @GET
    @Path("/{clubId}/layout")
    public Response getLayout(@PathParam("clubId") UUID clubId) {
        return clubLayoutService.getLayoutByClub(clubId)
                .map(layout -> Response.ok(layout).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{clubId}/layout/image")
    @Produces("image/*")
    public Response getLayoutImage(@PathParam("clubId") UUID clubId) {
        return clubLayoutService.getLayoutByClub(clubId)
                .map(layout -> Response.ok(layout.getData())
                        .type(layout.getContentType() != null ? layout.getContentType() : "application/octet-stream")
                        .build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }


    @GET
    @Path("/{clubId}/tables")
    public Response listTables(@PathParam("clubId") UUID clubId) {
        final List<ClubTable> tables = tableService.findByClubId(clubId);
        GenericEntity<List<ClubTable>> entity = new GenericEntity<>(tables) {
        };
        return Response.ok(entity).build();
    }


    @GET
    @Path("/{clubId}/menu")
    public Response getMenu(@PathParam("clubId") UUID clubId) {
        final List<Product> menu = menuService.getMenuByClub(clubId);
        if (menu.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(menu).build();
    }

    @GET
    @Path("/{clubId}/limits")
    public Response getProductAndTableLimits(@PathParam("clubId") UUID clubId) {
        boolean maxProductsReached = !productService.canAddProduct(clubId);
        boolean maxTablesReached = !tableService.canAddTable(clubId);

        Map<String, Boolean> result = new HashMap<>();
        result.put("maxProductsReached", maxProductsReached);
        result.put("maxTablesReached", maxTablesReached);
        return Response.ok(result).build();
    }


    @GET
    @Path("/{clubId}/reviews")
    public Response listReviews(@PathParam("clubId") UUID clubId,
                                @QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("size") @DefaultValue("10") int size) {

        page = normalizePage(page);
        size = normalizeSize(size);
        int pageIndex = page - 1;

        final List<Review> reviews = reviewService.findByClubPaginated(clubId, pageIndex, size);
        final int totalReviewsPages = reviewService.gettotalReviewsPages(clubId, size);

        Map<String, Object> payload = new HashMap<>();
        payload.put("items", reviews);
        payload.put("page", page);
        payload.put("size", size);
        payload.put("totalPages", totalReviewsPages);

        return Response.ok(payload).build();
    }


    @GET
    @Path("/events/upcoming")
    public Response listUpcomingEventsByClub(@QueryParam("page") @DefaultValue("0") int page,
                                             @QueryParam("size") @DefaultValue("30") int size) {

        page = Math.max(page, 0);
        size = normalizeSize(size);

        final List<Event> upcoming = eventService.listUpcoming(page, size);

        final Map<UUID, List<Event>> eventsByClub = upcoming.stream()
                .collect(Collectors.groupingBy(Event::getClubId));

        final List<Club> clubsForEvents = eventsByClub.keySet().stream()
                .map(id -> clubService.getClubById(id).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        c -> eventsByClub.get(c.getClubId()).stream()
                                .map(Event::getDate)
                                .min(Comparator.naturalOrder())
                                .orElse(LocalDate.MAX)
                ))
                .collect(Collectors.toList());

        Map<String, Object> payload = new HashMap<>();
        payload.put("eventsByClub", eventsByClub);
        payload.put("clubsForEvents", clubsForEvents);

        return Response.ok(payload).build();
    }


    private int normalizePage(final int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(final int size) {
        return size < 1 ? DEFAULT_SIZE : size;
    }

    private String normalizePriceRange(final String priceRange) {
        if (priceRange == null || priceRange.isBlank()) {
            return priceRange;
        }
        return priceRange.toUpperCase();
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
}
