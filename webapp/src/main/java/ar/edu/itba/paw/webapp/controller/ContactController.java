package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.MailService;
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
    private final MailService mailService;

    public ContactController(ClubService clubService, MailService mailService) {
        this.clubService = clubService;
        this.mailService = mailService;
    }

    @GetMapping("/contact")
    public String showContact(@RequestParam("id") UUID clubId, Model model) {
        Club club = clubService.getClubById(clubId).orElse(null);
        if (club == null) throw new NotFoundException("El boliche no existe.");
        ClubForm clubForm = new ClubForm();
        clubForm.setClubId(clubId);
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
        var clubOpt = clubService.getClubById(clubId);
        if (clubOpt.isEmpty()) return "error/404";
        var club = clubOpt.get();

        model.addAttribute("clubId", club.getClubId());
        model.addAttribute("clubName", club.getClubName());

        if (bindingResult.hasErrors()) {
            return "contact";
        }

        String to = club.getContactInfo() != null ? club.getContactInfo().getEmail() : "test@local";
        boolean sent = mailService.sendContact(
                to,
                club.getClubName(),
                clubForm.getEmail(),
                clubForm.getName(),
                clubForm.getPhone(),
                clubForm.getMessage()
        );

        model.addAttribute("sent", sent);
        if (!sent) {
            model.addAttribute("error", "Email send failed (see logs)");
        }else {
            model.addAttribute("clubForm", new ClubForm());
        }
        return "contact";
    }


    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("id") Long userId, Model model) {
        //a futuro verificara
        return "verifiedMail";
    }
}