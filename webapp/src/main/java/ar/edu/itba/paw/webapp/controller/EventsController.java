package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
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
    private final TableService tableService;
    private final MenuService menuService;
    private final ClubLayoutService clubLayoutService;
    private final ProductService productService;



    @Autowired
    public EventsController(EventService eventService, SecurityService securityService, ClubService clubService, PhotoService photoService, TableService tableService, MenuService menuService, ClubLayoutService clubLayoutService, ProductService productService) {
        this.eventService = eventService;
        this.securityService = securityService;
        this.clubService = clubService;
        this.photoService = photoService;
        this.tableService = tableService;
        this.menuService = menuService;
        this.clubLayoutService = clubLayoutService;
        this.productService = productService;
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
            model.addAttribute("editField", "event");
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
            model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(club));
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
            model.addAttribute("editField", "event");
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
            model.addAttribute("errors", errors);
            model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(club));
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

    // En EventsController.java
    private void preloadClubHandler(Model model, UUID clubId) {
        var club = clubService.getClubById(clubId).orElseThrow();
        model.addAttribute("club", club);
        model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());

        var photos = photoService.listByClub(clubId, 0, 50);
        var eventPhotoIds = eventService.getEventPhotoIds(clubId);
        var clubOnlyPhotos = photos.stream()
                .filter(p -> p.getPhotoId() != null && !eventPhotoIds.contains(p.getPhotoId()))
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() != null && p2.getCreatedAt() != null) {
                        return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                    }
                    return p2.getPhotoId().compareTo(p1.getPhotoId());
                })
                .toList();

        var carouselPhotos = photoService.getCarouselPhotos(clubId, 10, 0);
        var tableLayout = clubLayoutService.getLayoutByClub(clubId);
        String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);

        System.out.println("carouselPhotos URLs: " + carouselPhotos.stream().map(Photo::getUrl).toList());
        System.out.println("tableLayoutUrl: " + tableLayoutUrl);

        model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
        model.addAttribute("carouselPhotos", carouselPhotos);
        model.addAttribute("tableLayoutUrl", tableLayoutUrl);
    }
}