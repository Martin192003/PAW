package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.model.Club;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    // GET /webapp/clubs  (y /webapp/clubs/)
    @GetMapping({"", "/"})
    public String getAllClubs(Model model) {
        model.addAttribute("clubs", clubService.getAllClubs());
        return "index"; // /WEB-INF/jsp/index.jsp
    }


}
