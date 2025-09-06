package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/clubs")
public class ClubController {

    private final ClubService clubService;
    private  final PhotoService photoService;

    @Autowired
    public ClubController(ClubService clubService, PhotoService photoService) {
        this.photoService = photoService;
        this.clubService = clubService;
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






}
