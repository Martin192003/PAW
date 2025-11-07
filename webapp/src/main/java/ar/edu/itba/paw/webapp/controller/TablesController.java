package ar.edu.itba.paw.webapp.controller;


import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/tables")
public class TablesController {

     private final TableService tableService;
     private  final SecurityService securityService;
     private final ClubService clubService;
     private final PhotoService photoService;
     private final MenuService menuService;
     private final EventService eventService;
     private final ClubLayoutService clubLayoutService;

     @Autowired
     public TablesController(TableService tableService, SecurityService securityService, ClubService clubService, PhotoService photoService, MenuService menuService, EventService eventService, ClubLayoutService clubLayoutService) {
         this.tableService = tableService;
         this.securityService = securityService;
         this.clubService = clubService;
         this.photoService = photoService;
         this.menuService = menuService;
         this.eventService = eventService;
         this.clubLayoutService = clubLayoutService;
     }


    @PostMapping("/{tableId}/edit")
    public String editTable(
            @PathVariable UUID tableId,
            @Valid @ModelAttribute("editTableForm") EditTableForm form,
            BindingResult errors,
            RedirectAttributes ra,
            Model model
    ) {
        UUID clubId = form.getClubId();
        checkLoggedUser(clubId);

        if (errors.hasErrors()) {
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(clubId));
            model.addAttribute("menu", menuService.getMenuByClub(clubId));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl", photoService.listByClub(clubId, 0, 1)
                    .stream()
                    .findFirst()
                    .map(p -> p.getUrl())
                    .orElse(null));
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", form);
            model.addAttribute("editField", "table");
            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            EventAddForm eventForm = new EventAddForm();
            eventForm.setClubId(club.getClubId());
            model.addAttribute("eventForm", eventForm);
            EventEditForm editEventForm = new EventEditForm();
            editEventForm.setClubId(club.getClubId());
            model.addAttribute("editEventForm", editEventForm);
            model.addAttribute("errors", errors);
            model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
            var carouselPhotos = photoService.getCarouselPhotos(clubId, 10, 0);
            var tableLayout = clubLayoutService.getLayoutByClub(clubId);
            String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);

            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
            model.addAttribute("carouselPhotos", carouselPhotos);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);
            return "clubHandler";
        }

        try {
            tableService.update(
                    tableId,
                    clubId,
                    form.getTableNumber(),
                    form.getCapacity(),
                    TableState.DISPONIBLE,
                    form.getPrice(),
                    form.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            ra.addAttribute("err", "dup");
        } catch (ValidationException e) {
            ra.addAttribute("err", "price_max");
        } catch (InvalidTableNumberException e) {
            ra.addAttribute("err", "table_number");
        } catch (InvalidCapacityException e) {
            ra.addAttribute("err", "capacity");
        } catch (InvalidPriceException e) {
            ra.addAttribute("err", "price");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("err", "discount");
        }
        return "redirect:/clubs/handle?clubId=" + clubId;
    }


    @PostMapping("/{tableId}/delete")
    public String deleteTable(@PathVariable UUID tableId, @RequestParam("clubId") UUID clubId) {
        checkLoggedUser(clubId);
        tableService.delete(tableId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    private void checkLoggedUser(UUID clubId){
        User user = securityService.getVerifiedLoggedUser();
        if(user == null) {
            throw new UserNotFoundException("error.no_loggeduser");
        }
        List<User> clubUsers = clubService.getAdminsByClub(clubId);
        if(clubUsers.stream().noneMatch(u -> u.getId().equals(user.getId()))) {
            throw new UnauthorizedException("error.notAdmin");
        }
    }

    @PostMapping("/addTable")
    public String addTable(
            @Valid @ModelAttribute("tableForm") AddTableForm tableForm,
            BindingResult errors,
            Model model
    ) {
        UUID clubId = tableForm.getClubId();
        checkLoggedUser(clubId);

        if (tableForm.getDiscount() == null) {
            tableForm.setDiscount(BigDecimal.ZERO);
        }

        if (errors.hasErrors()) {
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(clubId));
            model.addAttribute("menu", menuService.getMenuByClub(clubId));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl",
                    photoService.listByClub(clubId, 0, 1).stream()
                            .findFirst().map(p -> p.getUrl()).orElse(null));

            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            EventAddForm eventForm = new EventAddForm();
            eventForm.setClubId(club.getClubId());
            model.addAttribute("eventForm", eventForm);
            EventEditForm editEventForm = new EventEditForm();
            editEventForm.setClubId(club.getClubId());
            model.addAttribute("editEventForm", editEventForm);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("tableForm", tableForm);
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            return "clubHandler";
        }

        try {
            tableService.create(
                    tableForm.getTableNumber(),
                    tableForm.getCapacity(),
                    clubId,
                    TableState.DISPONIBLE,
                    tableForm.getPrice(),
                    tableForm.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            return "redirect:/clubs/handle?clubId=" + clubId + "&err=dup";
        }

        return "redirect:/clubs/handle?clubId=" + clubId;
    }

}
