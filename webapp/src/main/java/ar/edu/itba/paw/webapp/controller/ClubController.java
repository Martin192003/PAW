package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.webapp.form.AddClubForm;
import ar.edu.itba.paw.webapp.form.ClubForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService, UserService userService) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.userService = userService;
    }

    @GetMapping({"", "/"})
    public String getAllClubs(Model model) {
        List<Club> clubs = clubService.getAllClubs();

        Map<UUID, String> photoUrlByClub = new HashMap<>();
        for (Club c : clubs) {
            List<Photo> photos = photoService.listByClub(c.getClubId(), 0, 1);
            if (!photos.isEmpty()) {
                photoUrlByClub.put(c.getClubId(), photos.get(0).getUrl()); // ej: /photos/{id}
            }
        }

        model.addAttribute("clubs", clubs);
        model.addAttribute("photoUrlByClub", photoUrlByClub);
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
            @Valid @ModelAttribute("clubForm") AddClubForm addClubForm,
            BindingResult errors) {

        if (errors.hasErrors()) {
            return "club_form";
        }

        User contactInfo = userService.findById(UUID.fromString("8966c8cf-b910-4d61-b748-c0b77acd9d9a"));

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
                // Manejar error si es necesario
            }
        }

        return "redirect:/clubs/" + newClub.getClubId();
    }





}
