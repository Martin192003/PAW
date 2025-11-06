    package ar.edu.itba.paw.webapp.controller;

    import ar.edu.itba.paw.model.*;
    import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
    import ar.edu.itba.paw.model.exceptions.NotFoundException;
    import ar.edu.itba.paw.model.exceptions.ValidationException;
    import ar.edu.itba.paw.service.*;
    import ar.edu.itba.paw.webapp.form.ReviewForm;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.*;

    import javax.servlet.http.HttpServletRequest;
    import java.security.Principal;
    import java.time.*;
    import java.time.format.DateTimeParseException;
    import java.util.*;
    import java.util.stream.Collectors;
    import java.util.UUID;

    @Controller
    @RequestMapping("/clubs")
    public class ClubDetailsController {

        private final ClubService clubService;
        private final ProductService productsService;
        private final PhotoService photoService;
        private final TableService tableService;
        private final SecurityService securityService;
        private final ReviewService reviewService;
        private final ClubLayoutService clubLayoutService;
        private final BookingService bookingService;

        private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        private final Clock clock = Clock.system(zone);

        public ClubDetailsController(final ClubService clubService,
                                     final ProductService productsService,
                                     final PhotoService photoService,
                                     final TableService tableService,
                                     final SecurityService securityService,
                                     final ClubLayoutService clubLayoutService,
                                     final ReviewService reviewService,
                                     final BookingService bookingService) {
            this.clubService = clubService;
            this.productsService = productsService;
            this.photoService = photoService;
            this.tableService = tableService;
            this.securityService = securityService;
            this.clubLayoutService = clubLayoutService;
            this.reviewService = reviewService;
            this.bookingService = bookingService;
        }

        @GetMapping("/{clubId}")
        public String clubDetails(
                @PathVariable("clubId") UUID clubId,
                @RequestParam(name = "date", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                LocalDate date,
                HttpServletRequest request,
                @RequestParam(defaultValue = "created_desc") String sort,
                @RequestParam(defaultValue = "0") int offset,
                Model model) {

            var club = clubService.getClubById(clubId)
                    .orElseThrow(() -> new NotFoundException("El boliche no existe."));

            int limit  = 10;
            offset = Math.max(0, offset);

            boolean fromEvent = "true".equals(request.getParameter("fromEvent"));
            LocalDate ld;

            if (fromEvent && date != null) {
                // Viene de un evento → aceptar cualquier fecha
                ld = date;
                // Pero generamos allowedDateStrings con solo esa fecha
                List<String> allowedDateStrings = List.of(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                model.addAttribute("allowedDateStrings", allowedDateStrings);
            } else {
                // Caso normal → solo Jue/Vie/Sáb
                LocalDate defaultDate = nextAllowedFrom(LocalDate.now(zone));
                List<LocalDate> allowedDates = new ArrayList<>();
                LocalDate today = LocalDate.now(zone);
                for (int i = 0; i < 15; i++) {
                    LocalDate d = today.plusDays(i);
                    DayOfWeek day = d.getDayOfWeek();
                    if (day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                        allowedDates.add(d);
                    }
                }
                List<String> allowedDateStrings = allowedDates.stream()
                        .map(d -> d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .collect(Collectors.toList());

                model.addAttribute("allowedDateStrings", allowedDateStrings);
                ld = (date == null) ? defaultDate : date;
            }

            model.addAttribute("selectedDate", ld.toString());
            model.addAttribute("availableTables", List.of());

            OffsetDateTime start = null;
            OffsetDateTime end = null;
            List<ar.edu.itba.paw.model.ClubTable> available = List.of();

            try {
                if (tableService.isPastDate(ld, clock, zone)) {
                    model.addAttribute("error", "tables.pastdate");
                } else {
                    available = tableService.findAvailableOnDate(clubId, ld, zone, clock);
                    start = tableService.windowStart(ld, zone);
                    end = tableService.windowEnd(ld, zone);
                }
            } catch (ValidationException | InvalidDateRangeException ex) {
                model.addAttribute("error", "invalid.date");
            }

            model.addAttribute("availableTables", available);

            var products = productsService.findByClub(club).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                    .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            var grouped = products.stream().collect(Collectors.groupingBy(productsService::classifyCategory));
            var desired = List.of("Cervezas","Tragos","Champagne","Sin Alcohol");
            var ordered = new LinkedHashMap<String, List<Product>>();
            desired.forEach(cat -> {
                var list = grouped.remove(cat);
                if (list != null && !list.isEmpty()) ordered.put(cat, list);
            });
            grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                    .forEach(e -> ordered.put(e.getKey(), e.getValue()));
            if (ordered.isEmpty() && !products.isEmpty()) ordered.put("Tragos", products);

            String coverPhotoUrl = photoService.getCoverUrl(clubId)
                    .orElse("/resources/img/default-club-placeholder.png");

            String tableLayoutUrl = clubLayoutService.getLayoutByClub(clubId)
                    .map(ClubLayout::getUrl)
                    .orElse(null);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);

            User user = securityService.getVerifiedLoggedUser();
            if (user != null) {
                model.addAttribute("user", user);
            }
            boolean isOwner = user != null && clubService.getAdminsByClub(clubId).stream().anyMatch(u -> u.getId().equals(user.getId()));
            model.addAttribute("isOwner", isOwner);

            List<Photo> carouselPhotos = photoService.getClubPhotos(clubId, 10, 0);
            List<Photo> reviewPhotos = photoService.getReviewPhotosByClub(clubId, 20, 0);
            model.addAttribute("reviewPhotos", reviewPhotos);

            model.addAttribute("isLoggedIn", securityService.getVerifiedLoggedUser() != null);
            model.addAttribute("coverPhotoUrl", coverPhotoUrl);
            model.addAttribute("clubPhotos", carouselPhotos);

            model.addAttribute("latitude",-34.5891429);
            model.addAttribute("longitude",-58.3961641);
            model.addAttribute("club", club);
            model.addAttribute("productsByCategory", ordered);
            model.addAttribute("availableTables", available);
            model.addAttribute("availableTablesCount", available != null ? available.size() : 0);
            model.addAttribute("locationText", toLocationText(club.getLocation()));
            model.addAttribute("genreText", toGenreText(club.getMusicGenre() != null ? club.getMusicGenre().getLabel() : null));
            model.addAttribute("coverPhotoUrl", coverPhotoUrl);
            model.addAttribute("clubId", clubId);
            model.addAttribute("reviewForm", new ReviewForm());

            if (start != null && end != null) {
                model.addAttribute("dayStart", start);
                model.addAttribute("dayEnd", end);
            }

            List<Review> reviews = reviewService.listByClub(clubId, sort, limit, offset);
            int total = reviewService.countByClub(clubId);

            if (offset >= total && total > 0) {
                int lastPage = (int) Math.floor((total - 1) / (double) limit);
                offset = lastPage * limit;
                reviews = reviewService.listByClub(clubId, sort, limit, offset);
            }

            model.addAttribute("reviews", reviews);
            model.addAttribute("totalReviews", total);
            model.addAttribute("limit", limit);
            model.addAttribute("offset", offset);
            model.addAttribute("sort", sort);
            model.addAttribute("hasMore", offset + reviews.size() < total);

            if (user != null) {
                Optional<Review> userReview = reviewService.getUserReviewForClub(user.getId(), clubId);
                model.addAttribute("userReview", userReview.orElse(null));
                model.addAttribute("isAuthenticated", true);
            } else {
                model.addAttribute("isAuthenticated", false);
            }

            return "clubDetails";
        }

        private String toLocationText(final String location) {
            return (location == null || location.isBlank()) ? "Sin ubicación" : location;
        }

        private String toGenreText(final String genre) {
            return (genre == null || genre.isBlank()) ? "—" : genre;
        }

        private LocalDate nextAllowedFrom(LocalDate from) {
            LocalDate d = from;
            int guard = 0;
            while (!tableService.isAllowedDay(d) || tableService.isPastDate(d, clock, zone)) {
                d = d.plusDays(1);
                if (++guard > 14) break;
            }
            return d;
        }
    }