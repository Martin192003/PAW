package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.model.Club;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/clubs")
public class ClubController {
    private final ClubService clubService;

    @Autowired
    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping("/")
    public String getAllClubs(Model model) {
        model.addAttribute("clubs", clubService.getAllClubs());
        return new ModelAndView "index";
    }

    @GetMapping("/{clubId}")
    public String getClubDetails(@PathVariable("clubId") UUID clubId, Model model){
        Optional<Club> club = clubService.getClubById(clubId);
        if(club.isPresent()){
            model.addAttribute("club", club.get());
            return "clubDetails";
        }
        //return "error/404";     //si no hay boliche manda error
        return "clubDetails";  //provisional
    }

}
