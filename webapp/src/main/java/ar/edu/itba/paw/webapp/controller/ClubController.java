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

import java.math.BigDecimal;
import java.io.IOException;
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

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, MenuService menuService, ProductService productService, SecurityService securityService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.menuService = menuService;
        this.productService = productService;
        this.securityService = securityService;
    }

    @GetMapping({"", "/"})
    public String list(
            Model model,
            @RequestParam(value = "name", required = false) String nameLike,
            @RequestParam(value = "zone", required = false) Zone zone,
            @RequestParam(value = "genre", required = false) String musicGenre,
            @RequestParam(value = "price", required = false) String priceRange,
            @RequestParam(value = "minRating", required = false) BigDecimal minRating,
            @RequestParam(value = "maxRating", required = false) BigDecimal maxRating
    ) {
        if (minRating != null && maxRating != null && minRating.compareTo(maxRating) > 0) {
            BigDecimal tmp = minRating; minRating = maxRating; maxRating = tmp;
        }

        boolean any = (nameLike != null || zone != null || musicGenre != null ||
                priceRange != null || minRating != null || maxRating != null);

        List<Club> clubs = any
                ? clubService.advancedSearch(nameLike, zone, musicGenre, priceRange, minRating, maxRating)
                : clubService.getAllClubs();

        Map<UUID, String> photoUrlByClub = new HashMap<>();
        for (Club c : clubs) {
            var photos = photoService.listByClub(c.getClubId(), 0, 1);
            if (!photos.isEmpty()) photoUrlByClub.put(c.getClubId(), photos.get(0).getUrl());
        }

        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
        model.addAttribute("name", nameLike);
        model.addAttribute("zone", zone);
        model.addAttribute("genre", musicGenre);
        model.addAttribute("price", priceRange);
        model.addAttribute("minRating", minRating);
        model.addAttribute("maxRating", maxRating);


        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());

        return "index";
    }

    @GetMapping("/new")
    public String showClubForm(Model model) {

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
                addClubForm.getZone()
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

        return "redirect:/clubs/" + newClub.getClubId();
    }

    @GetMapping("/handle")
    public String handleClub(
            @RequestParam("clubId") UUID clubId,
            Model model
    ) {
        var maybeClub = clubService.getClubById(clubId);
        if (maybeClub.isEmpty()) throw new NotFoundException("El boliche no existe.");
        var club = maybeClub.get();

        var photos = photoService.listByClub(clubId, 0, 1);
        String coverPhotoUrl = photos.isEmpty() ? null : photos.get(0).getUrl();

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
        productService.update(productId, product.getClub().getClubId(), product.getProductName(),
                product.getDescription(), basePrice, product.getProductType(), product.getIsAvailable());
        return "redirect:/clubs/handle?clubId=" + product.getClub().getClubId();
    }

    @PostMapping("/deleteProduct")
    public String deleteProduct(@RequestParam("productId") UUID productId) {
        var product = productService.findById(productId).orElseThrow();
        productService.delete(productId);
        return "redirect:/clubs/handle?clubId=" + product.getClub().getClubId();
    }

    @PostMapping("/addProduct")
    public String addProduct(
            @RequestParam("clubId") UUID clubId,
            @RequestParam("productName") String name,
            @RequestParam("description") String description,
            @RequestParam("basePrice") BigDecimal basePrice,
            @RequestParam("productType") String type
    ) {
        User user = securityService.getVerifiedLoggedUser();
        if(user == null) {
            throw new UserNotFoundException("No se encontró el usuario logueado.");
        }

        List<User> clubUsers = clubService.getAdminsByClub(clubId);

        if(clubUsers.stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            throw new UnauthorizedException("El usuario no es admin del club.");
        }

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

}