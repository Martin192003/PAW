package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/events")
public class EventsController {
    private final EventService eventService;
    private final SecurityService securityService;
    private final ClubService clubService;
    private final PhotoService photoService;


    @Autowired
    public EventsController(EventService eventService, SecurityService securityService, ClubService clubService, PhotoService photoService) {
        this.eventService = eventService;
        this.securityService = securityService;
        this.clubService = clubService;
        this.photoService = photoService;
    }



    @PostMapping("/add")
    public String addEvent(
            @Valid @ModelAttribute("eventForm") EventAddForm form,
            BindingResult errors,
            Model model
    ) {
        UUID clubId = form.getClubId();
        checkLoggedUser(clubId);


        if (errors.hasErrors()) {
            preloadClubHandler(model, clubId);
            model.addAttribute("eventForm", form);
            model.addAttribute("editEventForm", new EventEditForm());
            model.addAttribute("editField", "Event");
            return "clubHandler";
        }


        eventService.create(
                clubId,
                form.getEventName(),
                form.getDescription(),
                form.getDate(),
                form.getPhotoId()
        );
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/{eventId}/edit")
    public String editEvent(
            @PathVariable UUID eventId,
            @Valid @ModelAttribute("editEventForm") EventEditForm form,
            BindingResult errors,
            Model model
    ) {
        UUID clubId = form.getClubId();
        checkLoggedUser(clubId);


        if (errors.hasErrors()) {
            preloadClubHandler(model, clubId);
            model.addAttribute("eventForm", new EventAddForm());
            model.addAttribute("editEventForm", form);
            model.addAttribute("editField", "Event");
            return "clubHandler";
        }


        eventService.update(
                clubId, eventId,
                form.getEventName(),
                form.getDescription(),
                form.getDate(),
                form.getPhotoId()
        );
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/{eventId}/delete")
    public String deleteEvent(@PathVariable UUID eventId, @RequestParam("clubId") UUID clubId) {
        checkLoggedUser(clubId);
        eventService.delete(clubId, eventId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    // --- Helpers ---
    private void checkLoggedUser(UUID clubId) {
        User user = securityService.getVerifiedLoggedUser();
        if (user == null) {
            throw new UserNotFoundException("No se encontró el usuario logueado.");
        }
        List<User> admins = clubService.getAdminsByClub(clubId);
        if (admins.stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            throw new UnauthorizedException("El usuario no es admin del club.");
        }
    }


    private void preloadClubHandler(Model model, UUID clubId) {
        var club = clubService.getClubById(clubId).orElseThrow();
        model.addAttribute("club", club);
        model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
        model.addAttribute("coverPhotoUrl", photoService.listByClub(clubId, 0, 1)
                .stream().findFirst().map(Photo::getUrl).orElse(null));
    }
}