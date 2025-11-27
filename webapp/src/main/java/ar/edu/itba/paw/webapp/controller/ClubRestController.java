//package ar.edu.itba.paw.webapp.controller;
//
//import ar.edu.itba.paw.model.Club;
//import ar.edu.itba.paw.model.Event;
//import ar.edu.itba.paw.model.Zone;
//import ar.edu.itba.paw.service.ClubService;
//import ar.edu.itba.paw.service.EventService;
//import ar.edu.itba.paw.service.TableService;
//import com.sun.mail.iap.Response;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.ws.rs.*;
//import javax.ws.rs.core.*;
//import java.net.URI;
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
//@Path("/clubs")
//@Component
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
//public class ClubRestController {
//
//    private static final int DEFAULT_PAGE = 1;
//    private static final int DEFAULT_SIZE = 10;
//    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
//
//    @Autowired
//    private ClubService clubService;
//
//    @Autowired
//    private EventService eventService;
//
//    @Autowired
//    private TableService tableService;
//
//    @Context
//    private UriInfo uriInfo;
//
//    @GET
//    public Response listClubs(@QueryParam("name") String nameLike,
//                              @QueryParam("zone") Zone zone,
//                              @QueryParam("genre") String musicGenre,
//                              @QueryParam("price") String priceRange,
//                              @QueryParam("isBar") Boolean isBar,
//                              @QueryParam("availableToday") Boolean availableToday,
//                              @QueryParam("sort") String sort,
//                              @QueryParam("page") @DefaultValue("1") int page,
//                              @QueryParam("size") @DefaultValue("10") int size) {
//
//        page = (page < 1) ? DEFAULT_PAGE : page;
//        size = (size < 1) ? DEFAULT_SIZE : size;
//        final String normalizedPriceRange = (priceRange == null || priceRange.isBlank()) ? null : priceRange.toUpperCase();
//
//        final boolean availabilityMode = isAvailabilityMode(availableToday, sort);
//        final LocalDate selectedDay = resolveSelectedDay(availabilityMode, sort);
//        final boolean invalidDay = availabilityMode && !tableService.isAllowedDay(selectedDay);
//
//        int totalClubs = 0;
//        List<Club> clubs = Collections.emptyList();
//
//        if (availabilityMode) {
//            if (!invalidDay) {
//                totalClubs = clubService.countSearchAvailable(nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay);
//                final int offset = (page - 1) * size;
//                clubs = clubService.searchAvailablePaginated(nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset);
//            }
//        } else {
//            totalClubs = clubService.countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
//            final int offset = (page - 1) * size;
//            clubs = clubService.searchPaginated(nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset);
//        }
//
//        final int totalPages = (int) Math.ceil((double) totalClubs / size);
//
//        GenericEntity<List<Club>> entity = new GenericEntity<>(clubs) {};
//
//        Response.ResponseBuilder rb = Response.ok(entity);
//
//        rb.header("X-Total-Results", totalClubs);
//        rb.header("X-Total-Pages", totalPages);
//        rb.header("X-Page", page);
//        rb.header("X-Size", size);
//        rb.header("X-Available-Today", availabilityMode);
//        rb.header("X-Invalid-Day", invalidDay);
//
//        return rb.build();
//    }
//
//
//    @GET
//    @Path("/events/upcoming")
//    public Response listUpcomingEventsByClub(@QueryParam("page") @DefaultValue("0") int page,
//                                             @QueryParam("size") @DefaultValue("30") int size) {
//
//        page = Math.max(page, 0);
//
//        final List<Event> upcoming = eventService.listUpcoming(page, size);
//
//        final Map<UUID, List<Event>> eventsByClub = upcoming.stream()
//                .collect(Collectors.groupingBy(Event::getClubId));
//
//        final List<Club> clubsForEvents = eventsByClub.keySet().stream()
//                .map(id -> clubService.getClubById(id).orElse(null))
//                .filter(Objects::nonNull)
//                .sorted(Comparator.comparing(
//                        c -> eventsByClub.get(c.getClubId()).stream()
//                                .map(Event::getDate)
//                                .min(Comparator.naturalOrder())
//                                .orElse(LocalDate.MAX)
//                ))
//                .collect(Collectors.toList());
//
//        Map<String, Object> result = new HashMap<>();
//        result.put("eventsByClub", eventsByClub);
//        result.put("clubsForEvents", clubsForEvents);
//
//        return Response.ok(result).build();
//    }
//
//    private boolean isAvailabilityMode(final Boolean availableToday, final String sort) {
//        return (availableToday != null && availableToday)
//                || (sort != null && sort.startsWith("availability_"));
//    }
//
//    private LocalDate resolveSelectedDay(final boolean availabilityMode, final String sort) {
//        if (!availabilityMode) {
//            return null;
//        }
//        final Integer dayOffset = extractDayOffsetFromSort(sort);
//        return (dayOffset != null)
//                ? LocalDate.now(BUENOS_AIRES_ZONE).plusDays(dayOffset)
//                : LocalDate.now(BUENOS_AIRES_ZONE);
//    }
//
//    private Integer extractDayOffsetFromSort(final String sort) {
//        if (sort == null) {
//            return null;
//        }
//        // Regex para extraer d0, d1, d2 del sort string
//        final Pattern pattern = Pattern.compile("availability_d([0-2])_(?:asc|desc)");
//        final Matcher matcher = pattern.matcher(sort);
//        if (matcher.matches()) {
//            return Integer.parseInt(matcher.group(1));
//        }
//        return null;
//    }
//}