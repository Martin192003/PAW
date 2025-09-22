package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.AddClubForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

import javax.validation.Valid;
import java.util.*;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final PhotoService photoService;
    private final UserService userService;
    private final MenuService menuService;
    private final ProductService productService;
    private final SecurityService securityService;
    private final TableService tableService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, MenuService menuService, ProductService productService, SecurityService securityService, TableService tableService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.menuService = menuService;
        this.productService = productService;
        this.securityService = securityService;
        this.tableService = tableService;
    }

    @GetMapping({"", "/"})
    public String list(
            Model model,
            @RequestParam(value = "name", required = false) String nameLike,
            @RequestParam(value = "zone", required = false) Zone zone,
            @RequestParam(value = "genre", required = false) String musicGenre,
            @RequestParam(value = "price", required = false) String priceRange,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "isBar", required = false) Boolean isBar
    ) {


        boolean any = (nameLike != null || zone != null || musicGenre != null ||
                priceRange != null || sort != null || isBar != null);

        List<Club> clubs = any
                ? clubService.advancedSearch(nameLike, zone, musicGenre, priceRange, isBar, sort)
                : clubService.getAllClubs();

        Map<UUID, String> photoUrlByClub = new HashMap<>();
        for (Club c : clubs) {
            var photos = photoService.listByClub(c.getClubId(), 0, 1);
            if (!photos.isEmpty()) photoUrlByClub.put(c.getClubId(), photos.get(0).getUrl());
        }

        User user = securityService.getVerifiedLoggedUser();
        if(user!=null) {
            model.addAttribute("user", user);
        }

        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("name", nameLike);
        model.addAttribute("zone", zone);
        model.addAttribute("genre", musicGenre);
        model.addAttribute("price", priceRange);
        model.addAttribute("sort", sort);
        model.addAttribute("isBar", isBar);

        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());

        return "index";
    }

    @GetMapping("/new")
    public String showClubForm(Model model) {

        User user = securityService.getVerifiedLoggedUser();
        if(user!=null) {
            model.addAttribute("user", user);
        }

        model.addAttribute("clubForm", new AddClubForm());
        model.addAttribute("postPath", "/clubs/create");
        model.addAttribute("priceValues", Price.values());
        model.addAttribute("genreValues", MusicalGenre.values());
        model.addAttribute("zoneValues", Zone.values());

        return "club_form";
    }


    @PostMapping("/create")
    public String createClub(
            Model model,
            @Valid @ModelAttribute("clubForm") AddClubForm addClubForm,
            BindingResult errors) {

        if (errors.hasErrors()) {
            model.addAttribute("postPath", "/clubs/create");
            model.addAttribute("priceValues", Price.values());
            model.addAttribute("genreValues", MusicalGenre.values());
            model.addAttribute("zoneValues", Zone.values());
            return "club_form";
        }

        User contactInfo = securityService.getVerifiedLoggedUser();
        if(contactInfo == null) {
            throw new IllegalStateException("No se encontró el usuario de contacto por defecto.");
        }
        Club newClub = clubService.create(
                addClubForm.getName(),
                addClubForm.getDescription(),
                addClubForm.getLocation(),
                addClubForm.getPriceRange(),
                contactInfo,
                addClubForm.getMusicGenre(),
                addClubForm.getZone(),
                addClubForm.getIsBar()
        );

        var image = addClubForm.getImage();
        if (image != null && !image.isEmpty()) {
            try {
                photoService.upload(
                        newClub.getClubId(),
                        contactInfo.getId(),
                        image.getOriginalFilename(),
                        image.getContentType() != null ? image.getContentType() : "application/octet-stream",
                        image.getBytes()
                );
            } catch (IOException e) {
                throw new BadRequestException("Error al crear el imagen");
            }
        }
        if (newClub == null) {
            throw new DuplicateEntityException("No se pudo crear el club");
        }

        return "redirect:/clubs/handle?clubId=" + newClub.getClubId();
    }

    @GetMapping("/handle")
    public String handleClub(
            @RequestParam("clubId") UUID clubId,
            Model model
    ) {
        checkLoggedUser(clubId);

        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");
        var club = maybeClub.get();

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();

        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("club", club);
        model.addAttribute("coverPhotoUrl", coverPhotoUrl);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("menu", menuService.getMenuByClub(clubId));

        return "clubHandler";
    }

    @PostMapping("/editField")
    public String editField(
            @RequestParam("clubId") UUID clubId,
            @RequestParam("field") String field,
            @RequestParam("value") String value,
            Model model
    ) {
        checkLoggedUser(clubId);

        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");
        Club club = maybeClub.get();

        switch (field) {
            case "clubName":
                club.setClubName(value);
                break;
            case "location":
                club.setLocation(value);
                break;
            case "priceRange":
                club.setPriceRange(Price.valueOf(value.toUpperCase()));
                break;
            case "description":
                club.setDescription(value);
                break;
            case "zone":
                club.setZone(Zone.valueOf(value));
                break;
            case "musicGenre":
                club.setMusicGenre(MusicalGenre.valueOf(value));
                break;
            default:
                throw new IllegalArgumentException("Campo inválido: " + field);
        }

        clubService.updateClub(club);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @GetMapping("/activity")
    public String userClubs(
            @RequestParam("userId") UUID userId,
            Model model
    ) {
        List<Club> clubs = clubService.getClubsByUser(userId);

        Map<UUID, String> photoUrlByClub = new HashMap<>();
        for (Club c : clubs) {
            var photos = photoService.listByClub(c.getClubId(), 0, 1);
            if (!photos.isEmpty()) photoUrlByClub.put(c.getClubId(), photos.get(0).getUrl());
        }
        model.addAttribute("user", userService.findById(userId));
        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("activePage", "clubs");

        return "clubActivity";
    }

    @PostMapping("/editProductPrice")
    public String editProductPrice(
            @RequestParam("productId") UUID productId,
            @RequestParam("basePrice") BigDecimal basePrice
    ) {

        var product = productService.findById(productId).orElseThrow();
        UUID clubId = product.getClub().getClubId();

        checkLoggedUser(clubId);

        productService.update(productId, clubId, product.getProductName(),
                product.getDescription(), basePrice, product.getProductType(), product.getIsAvailable());
        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @PostMapping("/deleteProduct")
    public String deleteProduct(@RequestParam("productId") UUID productId) {

        var product = productService.findById(productId).orElseThrow();
        UUID clubId = product.getClub().getClubId();

        checkLoggedUser(clubId);

        productService.delete(productId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @PostMapping("/addProduct")
    public String addProduct(
            @RequestParam("clubId") UUID clubId,
            @RequestParam("productName") String name,
            @RequestParam("description") String description,
            @RequestParam("basePrice") BigDecimal basePrice,
            @RequestParam("productType") String type
    ) {
        checkLoggedUser(clubId);

        var club = clubService.getClubById(clubId).orElseThrow();
        productService.create(club, name, description, basePrice, ProductType.fromDb(type), true);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @GetMapping("/profile")
    public String showProfile(@RequestParam UUID userId, Model model) {
        User user = userService.findById(userId);
        if (user == null) {
            return "redirect:/error";
        }
        model.addAttribute("user", user);
        model.addAttribute("activePage", "profile");
        return "profile";
    }

    @PostMapping("/addTable")
    public String addTable(
            @RequestParam("clubId") UUID clubId,
            @RequestParam("tableNumber") int tableNumber,
            @RequestParam("capacity") int capacity,
            @RequestParam("price") BigDecimal price,
            @RequestParam("discount") BigDecimal discount
    ) {

        checkLoggedUser(clubId);

        if (tableNumber < 1 || capacity < 1 || price.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Datos inválidos para la mesa.");
        }

        var club = clubService.getClubById(clubId).orElseThrow();
        TableState state = TableState.DISPONIBLE; // Estado fijo
        tableService.create(tableNumber, capacity, club.getClubId(), state, price, discount);

        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @PostMapping("/tables/{tableId}/edit")
    public String editTable(
            @PathVariable UUID tableId,
            @RequestParam("clubId") UUID clubId,
            @RequestParam("tableNumber") int tableNumber,
            @RequestParam("capacity") int capacity,
            @RequestParam("price") BigDecimal price,
            @RequestParam("discount") BigDecimal discount
    ) {
        checkLoggedUser(clubId);

        tableService.update(tableId, clubId, tableNumber, capacity, TableState.DISPONIBLE, price, discount);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/tables/{tableId}/delete")
    public String deleteTable(@PathVariable UUID tableId, @RequestParam("clubId") UUID clubId) {
        checkLoggedUser(clubId);

        tableService.delete(tableId);
        return "redirect:/clubs/handle?clubId=" + clubId;
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



    /*
        @GetMapping("/club/tables")
        public String showTables(@RequestParam("userId") UUID userId, Model model) {
            User user = userService.findById(userId);
            List<Club> clubs = clubService.getClubsByUser(userId);
            List<Table> allTables = new ArrayList<>();
            for (Club club : clubs) {
                allTables.addAll(tableService.findByClubId(club.getClubId()));
            }
            model.addAttribute("tables", allTables);
            model.addAttribute("user", user);
            return "clubTables";
        }
    */

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


}