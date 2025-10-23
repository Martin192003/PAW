package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.exceptions.UnauthorizedException;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("eventForm", form);
            model.addAttribute("editEventForm", new EventEditForm());
            model.addAttribute("editField", "Event");
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
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
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Model model
    ) {
        UUID clubId = form.getClubId();
        checkLoggedUser(clubId);

        if (errors.hasErrors()) {
            preloadClubHandler(model, clubId);
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("eventForm", new EventAddForm());
            model.addAttribute("editEventForm", form);
            model.addAttribute("editField", "Event");
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            return "clubHandler";
        }

        if (photo != null && !photo.isEmpty()) {
            eventService.update(
                    clubId,
                    eventId,
                    form.getEventName(),
                    form.getDescription(),
                    form.getDate(),
                    photo
            );
        } else{
            eventService.update(
                    clubId,
                    eventId,
                    form.getEventName(),
                    form.getDescription(),
                    form.getDate(),
                    null
            );
        }

        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @PostMapping("/{eventId}/delete")
    public String deleteEvent(@PathVariable UUID eventId, @RequestParam("clubId") UUID clubId) {
        checkLoggedUser(clubId);
        eventService.delete(clubId, eventId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @GetMapping("/upcoming")
    public String listUpcoming(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model
    ) {
        var events = eventService.listUpcoming(page, size);
        int total = eventService.countUpcoming();
        int totalPages = (int) Math.ceil((double) total / size);

        model.addAttribute("events", events);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("total", total);
        model.addAttribute("totalPages", totalPages);
        return "eventsUpcoming";
    }

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

        // Fetch all photos for the club
        var photos = photoService.listByClub(clubId, 0, 50);
        var eventPhotoIds = eventService.getEventPhotoIds(clubId);

        // Filter photos to include only non-event photos and sort by recency
        var clubOnlyPhotos = photos.stream()
                .filter(p -> p.getPhotoId() != null && !eventPhotoIds.contains(p.getPhotoId()))
                .sorted((p1, p2) -> {
                    // Assuming Photo has a getCreatedAt() method returning LocalDateTime
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt()); // Newest first
                    }
                    // Fallback to photoId if createdAt is unavailable
                    return p2.getPhotoId().compareTo(p1.getPhotoId());
                })
                .collect(Collectors.toList());

        // Select cover photo
        String coverPhoto = clubOnlyPhotos.isEmpty()
                ? "/resources/img/default-club-placeholder.png"
                : clubOnlyPhotos.get(0).getUrl();

        model.addAttribute("coverPhotoUrl", coverPhoto);
        model.addAttribute("clubPhotos", clubOnlyPhotos);
    }

}