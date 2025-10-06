package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final PhotoService photoService;
    private final UserService userService;
    private final MenuService menuService;
    private final SecurityService securityService;
    private final TableService tableService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, MenuService menuService, ProductService productService, SecurityService securityService, TableService tableService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.menuService = menuService;
        this.securityService = securityService;
        this.tableService = tableService;
    }

    @GetMapping({"", "/"})
    public String list(@RequestParam(value = "name", required = false) String nameLike,
                       @RequestParam(value = "zone", required = false) Zone zone,
                       @RequestParam(value = "genre", required = false) String musicGenre,
                       @RequestParam(value = "price", required = false) String priceRange,
                       @RequestParam(value = "isBar", required = false) Boolean isBar,
                       @RequestParam(value = "sort", required = false) String sort,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       Model model) {

        if (size < 1) size = 10;
        if (page < 1) page = 1;

        int totalClubs = clubService.countSearch(nameLike, zone, musicGenre, priceRange, isBar);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalClubs / size));
        if (page > totalPages) page = totalPages;

        int offset = (page - 1) * size;

        List<Club> clubs = clubService.searchPaginated(
                nameLike, zone, musicGenre, priceRange, isBar, sort, size, offset
        );

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
        if (sort != null && !sort.isBlank()) filters.put("sort", sort);
        filters.put("size", String.valueOf(size)); // <--- importante
        String queryParams = filters.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);

        model.addAttribute("currentPage", page); // lo que ya tenías
        model.addAttribute("page", page);        // compatibilidad con JSP
        model.addAttribute("size", size);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", totalClubs);
        model.addAttribute("pages",
                java.util.stream.IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList())
        );

        model.addAttribute("queryParams", queryParams);
        model.addAttribute("name", nameLike);
        model.addAttribute("zone", zone);
        model.addAttribute("genre", musicGenre);
        model.addAttribute("price", priceRange);
        model.addAttribute("isBar", isBar);
        model.addAttribute("sort", sort);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());

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
        var role = contact.getRole();


        Club newClub = clubService.create(
                f.getName(), f.getDescription(), f.getLocation(), f.getPriceRange(),
                contact, f.getMusicGenre(), f.getZone(), f.getIsBar(), f.getImage()
        );

        if(role == Role.USER){
            return "clubCreatedModal";
        }

        return "redirect:/clubs/handle?clubId=" + newClub.getClubId();
    }

    @GetMapping("/handle")
    public String handleClub(@RequestParam("clubId") UUID clubId, Model model) {
        var user = securityService.getVerifiedLoggedUser();
        if (user == null) throw new UserNotFoundException("No logueado");
        clubService.assertAdmin(clubId, user.getId());

        var club = clubService.getClubById(clubId).orElseThrow(() -> new NotFoundException("El boliche no existe."));
        var cover = photoService.getCoverUrl(clubId).orElse(null);

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.getFirst().getUrl();


        model.addAttribute("club", club);
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("coverPhotoUrl", cover);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("productForm", new AddProductForm());
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("editTableForm", new EditTableForm());
        model.addAttribute("editProductForm", new EditProductForm());

        ClubHandlerForm form = new ClubHandlerForm();
        form.setClubId(club.getClubId());
        form.setName(club.getClubName());
        form.setLocation(club.getLocation());
        form.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
        form.setZone(club.getZone() != null ? club.getZone().name() : null);
        form.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
        form.setDescription(club.getDescription() != null ? club.getDescription() : "");
        form.setImage(coverPhotoUrl != null ? coverPhotoUrl : "");

        model.addAttribute("clubHandlerForm", form);
        return "clubHandler";
    }

    @PostMapping("/editClub")
    public String editClub(
            @Valid @ModelAttribute("clubHandlerForm") ClubHandlerForm clubHandlerForm,
            BindingResult errors,
            @RequestParam("clubId") UUID clubId,
            @RequestParam(value = "editedField", required = false) String editedField,
            Model model,
            RedirectAttributes ra
    ) {
        checkLoggedUser(clubId);

        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");
        Club club = maybeClub.get();

        if (errors.hasErrors()) {
            populateClubHandlerModel(model, club);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("editField", editedField);
            return "clubHandler";
        }

        if (clubHandlerForm.getName() != null && !clubHandlerForm.getName().isBlank()) {
            club.setClubName(clubHandlerForm.getName());
        }
        if (clubHandlerForm.getLocation() != null && !clubHandlerForm.getLocation().isBlank()) {
            club.setLocation(clubHandlerForm.getLocation());
        }
        if (clubHandlerForm.getZone() != null && !clubHandlerForm.getZone().isBlank()) {
            club.setZone(Zone.valueOf(clubHandlerForm.getZone()));
        }
        if (clubHandlerForm.getMusicGenre() != null && !clubHandlerForm.getMusicGenre().isBlank()) {
            club.setMusicGenre(MusicalGenre.valueOf(clubHandlerForm.getMusicGenre()));
        }
        if (clubHandlerForm.getPriceRange() != null && !clubHandlerForm.getPriceRange().isBlank()) {
            club.setPriceRange(Price.valueOf(clubHandlerForm.getPriceRange()));
        }
        if (clubHandlerForm.getDescription() != null) {
            club.setDescription(clubHandlerForm.getDescription());
        }

        clubService.updateClub(club);

        ra.addFlashAttribute("successMessage", "Cambios guardados correctamente.");

        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @GetMapping("/activity")
    public String userClubs(@RequestParam("userId") UUID userId,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            Model model) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        int offset = (page - 1) * size;

        List<Club> clubs = clubService.getClubsByUserPaginated(userId, size, offset);
        int totalClubs = clubService.countClubsByUser(userId);
        int totalPages = (int) Math.ceil((double) totalClubs / size);

        Map<UUID,String> photoUrlByClub = photoService.getCoverUrls(
                clubs.stream().map(Club::getClubId).collect(Collectors.toSet())
        );

        model.addAttribute("user", userService.findById(userId));
        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("activePage", "clubs");
        model.addAttribute("userId", userId);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalResults", totalClubs);
        return "clubActivity";
    }

    @PostMapping("/club/{clubId}/photos")
    public String upload(@PathVariable UUID clubId,
                         @RequestParam("file") MultipartFile file,
                         Principal principal) throws IOException {

        if (file == null || file.isEmpty()) {
            return "redirect:/clubs/handle?clubId=" + clubId + "&err=empty";
        }

        User user = securityService.getVerifiedLoggedUser();
        checkLoggedUser(clubId);

        UUID photoId = photoService.upload(
                clubId,
                user.getId(),
                file.getOriginalFilename(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getBytes()
        );

        return "redirect:/clubs/handle?clubId=" + clubId;
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
    private void populateClubHandlerModel(Model model, Club club) {
        UUID clubId = club.getClubId();
        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.getFirst().getUrl();

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
    }

    @PostMapping("/confirmClub")
    private String logout(){
        return "redirect:/logout";
    }

}