    package ar.edu.itba.paw.webapp.controller;

    import ar.edu.itba.paw.model.*;
    import ar.edu.itba.paw.model.exceptions.InvalidDateRangeException;
    import ar.edu.itba.paw.model.exceptions.NotFoundException;
    import ar.edu.itba.paw.model.exceptions.ValidationException;
    import ar.edu.itba.paw.service.*;
    import ar.edu.itba.paw.webapp.auth.PawUserDetails;
    import ar.edu.itba.paw.webapp.form.ReviewForm;
    import org.springframework.format.annotation.DateTimeFormat;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.*;

    import javax.servlet.http.HttpServletRequest;
    import java.time.*;
    import java.util.*;
    import java.util.UUID;

    @Controller
    @RequestMapping("/clubs")
    public class ClubDetailsController {

        private final ClubService clubService;
        private final ProductService productsService;
        private final PhotoService photoService;
        private final TableService tableService;
        private final ReviewService reviewService;
        private final ClubLayoutService clubLayoutService;

        private final ZoneId zone = ZoneId.of("America/Argentina/Buenos_Aires");
        private final Clock clock = Clock.system(zone);

        public ClubDetailsController(final ClubService clubService,
                                     final ProductService productsService,
                                     final PhotoService photoService,
                                     final TableService tableService,
                                     final ClubLayoutService clubLayoutService,
                                     final ReviewService reviewService) {
            this.clubService = clubService;
            this.productsService = productsService;
            this.photoService = photoService;
            this.tableService = tableService;
            this.clubLayoutService = clubLayoutService;
            this.reviewService = reviewService;
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
                Model model, @AuthenticationPrincipal PawUserDetails userDetails) {

            var club = clubService.getClubById(clubId).orElseThrow(() -> new NotFoundException("error.club.notfound"));

            int limit  = 10;
            offset = Math.max(0, offset);

            boolean fromEvent = "true".equals(request.getParameter("fromEvent"));

            LocalDate ld = tableService.resolveSelectedDate(date, fromEvent);
            model.addAttribute("allowedDateStrings", tableService.getAllowedDateStrings(LocalDate.now(zone), 15));
            model.addAttribute("selectedDate", ld.toString());
            model.addAttribute("availableTables", List.of());

            OffsetDateTime start = null;
            OffsetDateTime end = null;
            List<ClubTable> available = List.of();
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
            var ordered = productsService.getVisibleProductsByCategory(clubId);
            String coverPhotoUrl = photoService.getCoverUrl(clubId).orElse(null);
            String tableLayoutUrl = clubLayoutService.getLayoutByClub(clubId)
                    .map(ClubLayout::getUrl)
                    .orElse(null);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);

            if (userDetails != null) {
                model.addAttribute("user", userDetails.getUser());
                model.addAttribute("isLoggedIn", true);
                boolean isOwner = clubService.isUserOwner(clubId, userDetails.getUser());
                model.addAttribute("isOwner", isOwner);
            } else {
                model.addAttribute("isLoggedIn", false);
                model.addAttribute("isOwner", false);
            }
            List<Photo> carouselPhotos = photoService.getClubPhotos(clubId, 10, 0);
            List<Photo> reviewPhotos = photoService.getReviewPhotosByClub(clubId, 20, 0);
            model.addAttribute("reviewPhotos", reviewPhotos);
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
            model.addAttribute("hasReviewed", reviewService.getUserReviewForClub(userDetails.getId(), clubId).isPresent());

            if (start != null && end != null) {
                model.addAttribute("dayStart", start);
                model.addAttribute("dayEnd", end);
            }
            int total = reviewService.countByClub(clubId);
            int correctedOffset = reviewService.getCorrectedOffset(clubId,limit, offset);
            List<Review> reviews = reviewService.listByClubPaginated(clubId, sort, limit, offset);
            model.addAttribute("reviews", reviews);
            model.addAttribute("totalReviews", total);
            model.addAttribute("limit", limit);
            model.addAttribute("offset", correctedOffset);
            model.addAttribute("sort", sort);
            model.addAttribute("hasMore", correctedOffset + reviews.size() < total);
            if (userDetails != null) {
                Optional<Review> userReview = reviewService.getUserReviewForClub(userDetails.getId(), clubId);
                model.addAttribute("userReview", userReview.orElse(null));
                model.addAttribute("isAuthenticated", true);
            } else {
                model.addAttribute("isAuthenticated", false);
            }

            return "clubDetails";
        }

        private String toLocationText(final String location) {
            return (location == null || location.isBlank()) ? "error.notLocation" : location;
        }

        private String toGenreText(final String genre) {
            return (genre == null || genre.isBlank()) ? "—" : genre;
        }
    }