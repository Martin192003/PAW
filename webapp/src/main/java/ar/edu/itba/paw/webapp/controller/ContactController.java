package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.MailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        if (club == null) return "error/404";
        model.addAttribute("clubId", club.getClubId());
        model.addAttribute("clubName", club.getClubName());
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam UUID clubId,
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                @RequestParam String message,
                                RedirectAttributes ra) {
        var clubOpt = clubService.getClubById(clubId);
        if (clubOpt.isEmpty()) return "error/404";
        var club = clubOpt.get();

        String to = club.getContactInfo() != null ? club.getContactInfo().getEmail() : "test@local";
        boolean sent = mailService.sendContact(
                to,
                club.getClubName(),
                email,
                name,
                phone,
                message
        );

        if (sent) {
            // send confirmation to user (ignore result; non-blocking effect)
            mailService.sendContactConfirmation(email, club.getClubName(), message);
        }

        ra.addFlashAttribute("sent", sent);
        if (!sent) {
            ra.addFlashAttribute("error", "Email send failed (see logs)");
        }
        return "redirect:/contact?id=" + clubId;
    }
}