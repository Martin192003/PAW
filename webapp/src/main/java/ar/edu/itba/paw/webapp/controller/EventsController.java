package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/events")
public class EventsController {
    private final EventService eventService;
    private final ClubService clubService;
    private final PhotoService photoService;
    private final TableService tableService;
    private final MenuService menuService;
    private final ClubLayoutService clubLayoutService;
    private final ProductService productService;



    @Autowired
    public EventsController(EventService eventService, ClubService clubService, PhotoService photoService, TableService tableService, MenuService menuService, ClubLayoutService clubLayoutService, ProductService productService) {
        this.eventService = eventService;
        this.clubService = clubService;
        this.photoService = photoService;
        this.tableService = tableService;
        this.menuService = menuService;
        this.clubLayoutService = clubLayoutService;
        this.productService = productService;
    }



    @PostMapping("/add")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String addEvent(
            @Valid @ModelAttribute("eventForm") EventAddForm form,
            BindingResult errors,
            Model model
    ) throws IOException {
        UUID clubId = form.getClubId();

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

        Photo photo = null;
        if (form.getPhotoId() != null && !form.getPhotoId().isEmpty()) {
            photo = new Photo();
            photo.setFilename(form.getPhotoId().getOriginalFilename());
            photo.setContentType(form.getPhotoId().getContentType());
            photo.setData(form.getPhotoId().getBytes());
        }

        eventService.create(
                clubId,
                form.getEventName(),
                form.getDescription(),
                form.getDate(),
                photo
        );
        return "redirect:/clubs/handle?clubId=" + clubId + "&page=1";
    }


    @PostMapping("/{eventId}/edit")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String editEvent(
            @PathVariable UUID eventId,
            @Valid @ModelAttribute("editEventForm") EventEditForm form,
            BindingResult errors,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Model model
    ) throws IOException {
        UUID clubId = form.getClubId();
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

        Photo photoData = null;
        if (photo != null && !photo.isEmpty()) {
            photoData = new Photo();
            photoData.setFilename(photo.getOriginalFilename());
            photoData.setContentType(photo.getContentType());
            photoData.setData(photo.getBytes());
        }

        eventService.update(
                clubId,
                eventId,
                form.getEventName(),
                form.getDescription(),
                form.getDate(),
                photoData
        );

        return "redirect:/clubs/handle?clubId=" + clubId + "&page=1";
    }


    @PostMapping("/{eventId}/delete")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String deleteEvent(@PathVariable UUID eventId, @RequestParam("clubId") UUID clubId) {
        eventService.delete(clubId, eventId);
        return "redirect:/clubs/handle?clubId=" + clubId + "&page=1";
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

    private void preloadClubHandler(Model model, UUID clubId) {
        var club = clubService.getClubById(clubId).orElseThrow();
        model.addAttribute("club", club);
        model.addAttribute("events", eventService.listByClub(clubId, 0, 10));
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        var carouselPhotos = photoService.getCarouselPhotos(clubId);
        var tableLayout = clubLayoutService.getLayoutByClub(clubId);
        String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);
        model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
        model.addAttribute("carouselPhotos", carouselPhotos);
        model.addAttribute("tableLayoutUrl", tableLayoutUrl);
    }

    @GetMapping("/page/{clubId}/{page}")
    @ResponseBody
    public List<Event> getEventPage(@PathVariable UUID clubId,
                                    @PathVariable int page) {
        int pageSize = 10;
        return eventService.listByClub(clubId, page, pageSize);
    }
}