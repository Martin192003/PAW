package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.exceptions.EmailWasntSentException;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.webapp.form.ClubForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.UUID;

@Controller
public class ContactController {

    private final ClubService clubService;

    public ContactController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping("/contact")
    public String showContact(@RequestParam("id") UUID clubId, Model model) {
        Club club = clubService.getClubById(clubId).orElse(null);
        if (club == null) {
            throw new NotFoundException("El boliche no existe.");
        }

        model.addAttribute("clubId", club.getClubId());
        model.addAttribute("clubName", club.getClubName());
        model.addAttribute("clubForm", new ClubForm());
        return "contact";
    }

    @PostMapping("/contact")
    public String sendContact(@RequestParam("id") UUID clubId,
                              @ModelAttribute("clubForm") @Valid ClubForm clubForm,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes ra) {

        Club club = clubService.getClubById(clubId).orElse(null);
        if (club == null) {
            throw new NotFoundException("El boliche no existe.");
        }

        model.addAttribute("clubId", club.getClubId());
        model.addAttribute("clubName", club.getClubName());

        if (bindingResult.hasErrors()) {
            return "contact";
        }

        boolean sent = clubService.sendContactMessage(
                clubId,
                clubForm.getEmail(),
                clubForm.getName(),
                clubForm.getPhone(),
                clubForm.getMessage()
        );

        if (!sent) {
            throw new EmailWasntSentException("No se pudo enviar el email, intente nuevamente más tarde.");
        }

        model.addAttribute("sent", true);
        model.addAttribute("clubForm", new ClubForm());
        return "contact";
    }
}