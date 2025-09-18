package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.SecurityService;
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
import ar.edu.itba.paw.service.UserService;

import javax.validation.Valid;
import java.util.*;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final PhotoService photoService;
    private final UserService userService;
    private final SecurityService securityService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService, SecurityService securityService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
        this.securityService = securityService;

    }

    @GetMapping({"", "/"})
    public String list(
            Model model,
            @RequestParam(value = "name", required = false) String nameLike,
            @RequestParam(value = "zone", required = false) Zone zone,             // ENUM ✔
            @RequestParam(value = "genre", required = false) String musicGenre,    // String ✔
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

        // repintar filtros
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
            }
        }

        return "redirect:/clubs/" + newClub.getClubId();
    }





}