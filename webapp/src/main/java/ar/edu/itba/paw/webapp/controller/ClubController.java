package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.auth.WebSecurity;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Collections;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final PhotoService photoService;
    private final UserService userService;
    private final MenuService menuService;
    private final TableService tableService;
    private final EventService eventService;
    private final ClubLayoutService clubLayoutService;
    private final ProductService productService;
    private final ReviewService reviewService;
    private final WebSecurity webSecurity;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, MenuService menuService, TableService tableService, ClubLayoutService clubLayoutService, EventService eventService, ProductService productService, ReviewService reviewService, WebSecurity webSecurity) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.menuService = menuService;
        this.reviewService = reviewService;
        this.tableService = tableService;
        this.eventService = eventService;
        this.clubLayoutService = clubLayoutService;
        this.productService = productService;
        this.webSecurity = webSecurity;
    }

    @GetMapping({"", "/"})
    public String list(@RequestParam(value = "name", required = false) String nameLike,
                       @RequestParam(value = "zone", required = false) Zone zone,
                       @RequestParam(value = "genre", required = false) String musicGenre,
                       @RequestParam(value = "price", required = false) String priceRange,
                       @RequestParam(value = "isBar", required = false) Boolean isBar,
                       @RequestParam(value = "availableToday", required = false) Boolean availableToday,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model, @AuthenticationPrincipal PawUserDetails userDetails) {

        if (size < 1) size = 10;
        if (page < 1) page = 1;

        String normalizedPriceRange = priceRange;
        if (priceRange != null && !priceRange.isBlank()) {
            normalizedPriceRange = priceRange.toUpperCase();
        }

        final boolean availabilityMode = (availableToday != null && availableToday)
                || (sort != null && sort.startsWith("availability_"));

        LocalDate selectedDay = null;
        var zoneBA = ZoneId.of("America/Argentina/Buenos_Aires");
        Integer dayOffset = extractDayOffsetFromSort(sort);
        if (availabilityMode) {
            selectedDay = (dayOffset != null)
                    ? LocalDate.now(zoneBA).plusDays(dayOffset)
                    : LocalDate.now(zoneBA);
        }

        boolean invalidDay = availabilityMode && !tableService.isAllowedDay(selectedDay);
        model.addAttribute("invalidDay", invalidDay);
        model.addAttribute("selectedAvailabilityDate", selectedDay);

        int totalClubs = 0;
        int offset;
        List<Club> clubs = Collections.emptyList();
        int totalPages;

        if (availabilityMode) {
            if (!invalidDay) {
                totalClubs = clubService.countSearchAvailable(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay
                );

                if (userDetails != null) {
                    totalPages = clubService.countPagesForUser(userDetails.getUser().getId(), size);
                    offset = clubService.calculateOffset(page, userDetails.getUser().getId(), size);
                } else {
                    totalPages = clubService.pagesForCount(size, nameLike, zone, musicGenre, normalizedPriceRange, isBar);
                    offset = clubService.offsetForPage(page, size, nameLike, zone, musicGenre, normalizedPriceRange, isBar);
                }
                clubs = clubService.searchAvailablePaginated(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset
                );

                model.addAttribute("totalPages", totalPages);
            } else {
                model.addAttribute("totalPages", 1);
            }
        } else {
            totalClubs = clubService.countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
            if (userDetails != null) {
                totalPages = clubService.countPagesForUser(userDetails.getUser().getId(), size);
                offset = clubService.calculateOffset(page, userDetails.getUser().getId(), size);
            } else {
                totalPages = clubService.pagesForCount(size,nameLike, zone, musicGenre, normalizedPriceRange, isBar);
                offset = clubService.offsetForPage(page, size, nameLike, zone, musicGenre, normalizedPriceRange, isBar);
            }
            clubs = clubService.searchPaginated(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset
            );
            model.addAttribute("totalPages", totalPages);
        }

        Map<UUID, String> photoUrlByClub = photoService.getCoverUrls(
                clubs.stream().map(Club::getClubId).collect(Collectors.toSet())
        );
        if (userDetails != null) {
            model.addAttribute("user", userDetails.getUser());
        }

        Map<String, String> filters = new LinkedHashMap<>();
        if (nameLike != null && !nameLike.isBlank()) filters.put("name", nameLike);
        if (zone != null) filters.put("zone", zone.name());
        if (musicGenre != null && !musicGenre.isBlank()) filters.put("genre", musicGenre);
        if (priceRange != null && !priceRange.isBlank()) filters.put("price", priceRange);
        if (isBar != null) filters.put("isBar", isBar.toString());
        if (availabilityMode) filters.put("availableToday", "true");
        if (sort != null && !sort.isBlank()) filters.put("sort", sort);
        filters.put("size", String.valueOf(size));

        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        if (userDetails != null){
            totalPages = clubService.countPagesForUser(userDetails.getUser().getId(), size);
        }else{
            totalPages = clubService.pagesForCount(size,nameLike, zone, musicGenre, normalizedPriceRange, isBar);
        }
        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("currentPage", page);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", totalClubs);
        model.addAttribute("pages", IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList()));
        model.addAttribute("availableToday", availabilityMode);
        model.addAttribute("queryParams", queryParams);
        model.addAttribute("name", nameLike);
        model.addAttribute("zone", zone);
        model.addAttribute("genre", musicGenre);
        model.addAttribute("price", priceRange);
        model.addAttribute("isBar", isBar);
        model.addAttribute("sort", sort);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        addEventsByClubToModel(model);
        return "index";
    }

    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String showClubForm(Model model) {

        model.addAttribute("clubForm", new AddClubForm());
        model.addAttribute("postPath", "/clubs/create");
        model.addAttribute("priceValues", Price.values());
        model.addAttribute("genreValues", MusicalGenre.values());
        model.addAttribute("zoneValues", Zone.values());
        return "clubForm";
    }

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    public String createClub(Model model,
                             @Valid @ModelAttribute("clubForm") AddClubForm f,
                             BindingResult errors, @AuthenticationPrincipal PawUserDetails userDetails) throws IOException {
        if (errors.hasErrors()) {
            model.addAttribute("clubForm", f);
            model.addAttribute("postPath", "/clubs/create");
            model.addAttribute("priceValues", Price.values());
            model.addAttribute("genreValues", MusicalGenre.values());
            model.addAttribute("zoneValues", Zone.values());
            return "clubForm";
        }

        var roleAntes = userDetails.getUser().getRole();

        Photo photo = null;
        if (f.getImage() != null && !f.getImage().isEmpty()) {
            photo = photoService.createPhoto(
                    f.getImage().getOriginalFilename(),
                    f.getImage().getContentType(),
                    f.getImage().getBytes()
            );
        }
        Club newClub = clubService.create(
                f.getName(), f.getDescription(), f.getLocation(), f.getPriceRange(),
                userDetails.getUser(), f.getMusicGenre(), f.getZone(), f.getIsBar(), f.getCbu(), photo
        );

        if (roleAntes == Role.USER) {
            User updatedUser = userService.findById(userDetails.getId()).orElseThrow(UserNotFoundException::new);
            webSecurity.refreshAuthentication(updatedUser);
        }

        return "redirect:/clubs/handle?clubId=" + newClub.getClubId();
    }

    @GetMapping("/handle")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String handleClub(@RequestParam("clubId") UUID clubId,
                             @RequestParam(name = "page", defaultValue = "1") int page,
                             @RequestParam(name = "size", defaultValue = "10") int size,
                             @RequestParam(name = "filter", defaultValue = "all") String filter,
                             @RequestParam(name = "reviewPage", defaultValue = "1") int reviewPage,
                             Model model) {
        var club = clubService.getClubById(clubId).orElseThrow(() -> new NotFoundException("error.club.notfound"));

        String coverPhotoUrl = photoService.getCoverUrl(clubId)
                .orElse("/resources/img/default-club-placeholder.png");

        model.addAttribute("coverPhotoUrl", coverPhotoUrl);
        model.addAttribute("carouselPhotos", photoService.getCarouselPhotos(clubId));

        var layoutOpt = clubLayoutService.getLayoutByClub(clubId);
        String tableLayoutUrl = layoutOpt.map(ar.edu.itba.paw.model.ClubLayout::getUrl).orElse(null);


        int pageIndex = page - 1;
        List<Event> events;
        int totalPages = switch (filter) {
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
                yield eventService.totalEventPagesUpcoming(clubId, size);
            }
        };



        model.addAttribute("filter", filter);
        model.addAttribute("club", club);
        model.addAttribute("events", events);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);

        EventAddForm eventForm = new EventAddForm();
        eventForm.setClubId(club.getClubId());
        model.addAttribute("eventForm", eventForm);

        EventEditForm editEventForm = new EventEditForm();
        editEventForm.setClubId(club.getClubId());
        model.addAttribute("editEventForm", editEventForm);

        model.addAttribute("club", club);
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("tableLayoutUrl", tableLayoutUrl);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("productForm", new AddProductForm());
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("editTableForm", new EditTableForm());
        model.addAttribute("editProductForm", new EditProductForm());
        model.addAttribute("editField", "Event");
        model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
        model.addAttribute("maxTablesReached", !tableService.canAddTable(club));

        int reviewPageIndex = reviewPage - 1;
        List<Review> reviews = reviewService.findByClubPaginated(clubId, reviewPageIndex, size);
        int totalReviewsPages = reviewService.gettotalReviewsPages(clubId, size);

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("totalReviewsPages", totalReviewsPages);

        ClubHandlerForm form = new ClubHandlerForm();
        form.setClubId(club.getClubId());
        form.setName(club.getClubName());
        form.setLocation(club.getLocation());
        form.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
        form.setZone(club.getZone() != null ? club.getZone().name() : null);
        form.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
        form.setDescription(club.getDescription() != null ? club.getDescription() : "");
        form.setImage(coverPhotoUrl);
        form.setCbu(club.getCbu() != null ? club.getCbu() : "");


        model.addAttribute("clubHandlerForm", form);
        return "clubHandler";
    }

    @PostMapping("/editClub")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String editClub(
            @Valid @ModelAttribute("clubHandlerForm") ClubHandlerForm clubHandlerForm,
            BindingResult errors,
            @RequestParam("clubId") UUID clubId,
            @RequestParam(value = "editedField", required = false) String editedField,
            Model model,
            RedirectAttributes ra
    ) {
        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("error.club.notfound");
        Club club = maybeClub.get();

        if (errors.hasErrors()) {
            populateClubHandlerModel(model, clubId);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("editField", editedField);
            model.addAttribute("errors", errors);
            var carouselPhotos = photoService.getCarouselPhotos(clubId);
            model.addAttribute("carouselPhotos", carouselPhotos);
            return "clubHandler";
        }

        if ("clubName".equals(editedField)) {
            club.setClubName(clubHandlerForm.getName());
        } else if ("location".equals(editedField)) {
            club.setLocation(clubHandlerForm.getLocation());
        } else if ("zone".equals(editedField)) {
            club.setZone(Zone.valueOf(clubHandlerForm.getZone()));
        } else if ("musicGenre".equals(editedField)) {
            club.setMusicGenre(MusicalGenre.valueOf(clubHandlerForm.getMusicGenre()));
        } else if ("priceRange".equals(editedField)) {
            club.setPriceRange(Price.valueOf(clubHandlerForm.getPriceRange()));
        } else if ("description".equals(editedField)) {
            club.setDescription(clubHandlerForm.getDescription());
        } else if ("cbu".equals(editedField)) {
            club.setCbu(clubHandlerForm.getCbu());
        }

        clubService.updateClub(club);

        ra.addFlashAttribute("successMessage", "changes.changesCorrectly");

        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    private void populateClubHandlerModel(Model model, UUID clubId) {

        var layoutOpt = clubLayoutService.getLayoutByClub(clubId);
        String tableLayoutUrl = layoutOpt.map(ar.edu.itba.paw.model.ClubLayout::getUrl).orElse(null);

        model.addAttribute("tables", tableService.findByClubId(clubId));
        Club club = clubService.getClubByIdOrThrow(clubId);
        model.addAttribute("club", club);
        model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
        model.addAttribute("tableLayoutUrl", tableLayoutUrl);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("productForm", new AddProductForm());
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("editProductForm", new EditProductForm());
        model.addAttribute("editTableForm", new EditTableForm());
        EventAddForm eventForm = new EventAddForm();
        eventForm.setClubId(clubId);
        model.addAttribute("eventForm", eventForm);

        EventEditForm editEventForm = new EventEditForm();
        editEventForm.setClubId(clubId);
        model.addAttribute("editEventForm", editEventForm);
        model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
    }

    private Integer extractDayOffsetFromSort(String sort) {
        if (sort == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("availability_d([0-2])_(?:asc|desc)")
                .matcher(sort);
        if (m.matches()) {
            return Integer.parseInt(m.group(1));
        }
        return null;
    }

    private void addEventsByClubToModel(Model model) {
        final int page = 0;
        final int size = 30;
        List<Event> upcoming = eventService.listUpcoming(page, size);

        Map<UUID, List<Event>> eventsByClub = upcoming.stream()
                .collect(Collectors.groupingBy(Event::getClubId));

        List<Club> clubsForEvents = eventsByClub.keySet().stream()
                .map(id -> clubService.getClubById(id).orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        c -> eventsByClub.get(c.getClubId()).stream()
                                .map(Event::getDate)
                                .min(Comparator.naturalOrder())
                                .orElse(LocalDate.MAX)
                ))
                .collect(Collectors.toList());
        model.addAttribute("eventsByClub", eventsByClub);
        model.addAttribute("clubsForEvents", clubsForEvents);
    }

}