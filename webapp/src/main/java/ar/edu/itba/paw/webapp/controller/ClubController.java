package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Collections;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
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
    private final SecurityService securityService;
    private final TableService tableService;
    private final EventService eventService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, MenuService menuService, SecurityService securityService, TableService tableService, EventService eventService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.menuService = menuService;
        this.securityService = securityService;
        this.tableService = tableService;
        this.eventService = eventService;
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
                       Model model) {

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
                    : LocalDate.now(zoneBA); // fallback: hoy
        }

        // Valido día permitido
        boolean invalidDay = availabilityMode && (selectedDay == null || !tableService.isAllowedDay(selectedDay));
        model.addAttribute("invalidDay", invalidDay);
        model.addAttribute("selectedAvailabilityDate", selectedDay);

        int totalClubs = 0;
        int offset = (page - 1) * size;
        List<Club> clubs = Collections.emptyList();

        if (availabilityMode) {
            if (!invalidDay) {
                totalClubs = clubService.countSearchAvailable(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay
                );

                int totalPages = Math.max(1, (int) Math.ceil((double) totalClubs / size));
                if (page > totalPages) page = totalPages;
                offset = (page - 1) * size;

                clubs = clubService.searchAvailablePaginated(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset
                );

                model.addAttribute("totalPages", Math.max(1, (int) Math.ceil((double) totalClubs / size)));
            } else {
                model.addAttribute("totalPages", 1);
            }
        } else {
            totalClubs = clubService.countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
            int totalPages = Math.max(1, (int) Math.ceil((double) totalClubs / size));
            if (page > totalPages) page = totalPages;
            offset = (page - 1) * size;

            clubs = clubService.searchPaginated(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset
            );

            model.addAttribute("totalPages", totalPages);
        }

        Map<UUID, String> photoUrlByClub = photoService.getCoverUrls(
                clubs.stream().map(Club::getClubId).collect(Collectors.toSet())
        );

        var user = securityService.getVerifiedLoggedUser();
        if (user != null) model.addAttribute("user", user);

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

        // Modelo
        int totalPages = Math.max(1, (int) Math.ceil((double) totalClubs / size));
        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("currentPage", page);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", totalClubs);
        model.addAttribute("pages", IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList()));
        model.addAttribute("availableToday", availabilityMode); // reflejo modo disponibilidad
        model.addAttribute("queryParams", queryParams);
        model.addAttribute("name", nameLike);
        model.addAttribute("zone", zone);
        model.addAttribute("genre", musicGenre);
        model.addAttribute("price", priceRange);
        model.addAttribute("isBar", isBar);
        model.addAttribute("sort", sort);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        addEventsByClubToModel(clubs, model);
        return "index";
    }

    @GetMapping("/new")
    public String showClubForm(Model model) {

        User user = securityService.getVerifiedLoggedUser();
        if(user!=null) model.addAttribute("user", user);
        model.addAttribute("clubForm", new AddClubForm());
        model.addAttribute("postPath", "/clubs/create");
        model.addAttribute("priceValues", Price.values());
        model.addAttribute("genreValues", MusicalGenre.values());
        model.addAttribute("zoneValues", Zone.values());

        return "clubForm";
    }

    @PostMapping("/create")
    public String createClub(Model model,
                             @Valid @ModelAttribute("clubForm") AddClubForm f,
                             BindingResult errors) throws IOException {
        if (errors.hasErrors()) {
            model.addAttribute("clubForm", f);
            model.addAttribute("postPath", "/clubs/create");
            model.addAttribute("priceValues", Price.values());
            model.addAttribute("genreValues", MusicalGenre.values());
            model.addAttribute("zoneValues", Zone.values());
            return "clubForm";
        }

        var contact = securityService.getVerifiedLoggedUser();
        if (contact == null) throw new IllegalStateException("No se encontró el usuario de contacto.");
        var roleAntes = contact.getRole();

        Club newClub = clubService.create(
                f.getName(), f.getDescription(), f.getLocation(), f.getPriceRange(),
                contact, f.getMusicGenre(), f.getZone(), f.getIsBar(), f.getImage()
        );

        if (roleAntes == Role.USER) {
            User updatedUser = userService.findById(contact.getId());

            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUser.getUsername(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + updatedUser.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }

        return "redirect:/clubs/handle?clubId=" + newClub.getClubId();
    }

    @GetMapping("/handle")
    public String handleClub(@RequestParam("clubId") UUID clubId, Model model) {
        var user = securityService.getVerifiedLoggedUser();
        if (user == null) throw new UserNotFoundException("No logueado");
        clubService.assertAdmin(clubId, user.getId());

        var club = clubService.getClubById(clubId).orElseThrow(() -> new NotFoundException("El boliche no existe."));

        var photos = photoService.listByClub(clubId, 0, 50);

        var eventPhotoIds = eventService.getEventPhotoIds(clubId);

        //LÓGICA DE NEGOCIOS, PASAR A SERVICE
        var clubOnlyPhotos = photos.stream()
                .filter(p -> p.getPhotoId() != null && !eventPhotoIds.contains(p.getPhotoId()))
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    }
                    return p2.getPhotoId().compareTo(p1.getPhotoId());
                })
                .collect(Collectors.toList());

        String coverPhoto = clubOnlyPhotos.isEmpty()
                ? "/resources/img/default-club-placeholder.png"
                : clubOnlyPhotos.getFirst().getUrl();

        model.addAttribute("coverPhotoUrl", coverPhoto);
        model.addAttribute("clubPhotos", clubOnlyPhotos);

        model.addAttribute("events", eventService.listByClub(clubId, 0, 20));

        EventAddForm eventForm = new EventAddForm();
        eventForm.setClubId(club.getClubId());
        model.addAttribute("eventForm", eventForm);

        EventEditForm editEventForm = new EventEditForm();
        editEventForm.setClubId(club.getClubId());
        model.addAttribute("editEventForm", editEventForm);

        model.addAttribute("club", club);
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("coverPhotoUrl", coverPhoto);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("productForm", new AddProductForm());
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("editTableForm", new EditTableForm());
        model.addAttribute("editProductForm", new EditProductForm());
        model.addAttribute("editField", "Event");

        ClubHandlerForm form = new ClubHandlerForm();
        form.setClubId(club.getClubId());
        form.setName(club.getClubName());
        form.setLocation(club.getLocation());
        form.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
        form.setZone(club.getZone() != null ? club.getZone().name() : null);
        form.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
        form.setDescription(club.getDescription() != null ? club.getDescription() : "");
        form.setImage(coverPhoto != null ? coverPhoto : "");

        model.addAttribute("clubHandlerForm", form);
        return "clubHandler";
    }

    private void populateClubHandlerModel(Model model, Club club) {
        UUID clubId = club.getClubId();
        var photos = photoService.listByClub(clubId, 0, 50);
        var eventPhotoIds = eventService.getEventPhotoIds(clubId);
        var clubOnlyPhotos = photos.stream()
                .filter(p -> p.getPhotoId() != null && !eventPhotoIds.contains(p.getPhotoId()))
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt()); // Newest first
                    }
                    return p2.getPhotoId().compareTo(p1.getPhotoId());
                })
                .toList();

        String coverPhoto = clubOnlyPhotos.isEmpty()
                ? "/resources/img/default-club-placeholder.png"
                : clubOnlyPhotos.getFirst().getUrl();
        String coverPhotoUrl = clubOnlyPhotos.isEmpty() ? null : clubOnlyPhotos.get(0).getUrl();
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("club", club);
        model.addAttribute("coverPhotoUrl", coverPhotoUrl);
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

    private void checkLoggedUser(UUID clubId){
        User user = securityService.getVerifiedLoggedUser();
        if(user == null) {
            throw new UserNotFoundException("No se encontró el usuario logueado.");
        }
        List<User> clubUsers = clubService.getAdminsByClub(clubId);
        if(clubUsers.stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            throw new UnauthorizedException("El usuario no es admin del club.");
        }
    }
    @PostMapping("/confirmClub")
    private String logout(){
        return "redirect:/logout";
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

    private void addEventsByClubToModel(List<Club> clubs, Model model) {
        Map<UUID, List<Event>> eventsByClub = clubs.stream()
                .collect(Collectors.toMap(
                        Club::getClubId,
                        c -> eventService.listByClub(c.getClubId(), 0, 20).stream()
                                .filter(e -> !e.getDate().isBefore(LocalDate.now()))
                                .sorted(Comparator.comparing(Event::getDate))
                                .limit(3)
                                .collect(Collectors.toList())
                ));
        model.addAttribute("eventsByClub", eventsByClub);
    }


}