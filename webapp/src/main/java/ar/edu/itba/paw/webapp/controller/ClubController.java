package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private final PhotoService photoService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService) {
        this.clubService = clubService;
        this.photoService = photoService;
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
        return "club_form";
    }


    @PostMapping("/create")
    public String createClub(
            @Valid @ModelAttribute("clubForm") AddClubForm addClubForm,
            BindingResult errors) {

        if (errors.hasErrors()) {
            return "club_form";
        }

        User contactInfo = userService.findById(UUID.fromString("ed690532-e9d7-4a87-898b-2ca99bb9563d"));

        Club newClub = clubService.create(
                addClubForm.getName(),
                addClubForm.getDescription(),
                addClubForm.getLocation(),
                addClubForm.getPriceRange(),
                contactInfo,
                addClubForm.getMusicGenre(),
                addClubForm.getZone()
        );

        return "redirect:/clubs/" + newClub.getClubId();
    }



}