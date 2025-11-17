package ar.edu.itba.paw.webapp.controller;


import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ClubService clubService;
    private final PhotoService photoService;
    private final MenuService menuService;
    private final EventService eventService;
    private final ClubLayoutService clubLayoutService;
    private final ProductService productService;

    @Autowired
    public TablesController(TableService tableService, ClubService clubService, PhotoService photoService, MenuService menuService, EventService eventService, ClubLayoutService clubLayoutService, ProductService productService) {
            this.tableService = tableService;
            this.clubService = clubService;
            this.photoService = photoService;
            this.menuService = menuService;
            this.eventService = eventService;
            this.clubLayoutService = clubLayoutService;
            this.productService = productService;
        }

    @PostMapping("/{tableId}/edit")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String editTable(
            @PathVariable UUID tableId,
            @Valid @ModelAttribute("editTableForm") EditTableForm form,
            BindingResult errors,
            RedirectAttributes ra,
            Model model
    ) {
        if (errors.hasErrors()) {
            var club = clubService.getClubById(form.getClubId()).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(form.getClubId()));
            model.addAttribute("menu", menuService.getMenuByClub(form.getClubId()));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl", photoService.listByClub(form.getClubId(), 0, 1)
                    .stream()
                    .findFirst()
                    .map(Photo::getUrl)
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
            model.addAttribute("events", eventService.listByClub(form.getClubId(), 0, 20));
            var carouselPhotos = photoService.getCarouselPhotos(form.getClubId());
            var tableLayout = clubLayoutService.getLayoutByClub(form.getClubId());
            String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);

            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(form.getClubId()).orElse(null));
            model.addAttribute("carouselPhotos", carouselPhotos);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);
            model.addAttribute("maxProductsReached", !productService.canAddProduct(form.getClubId()));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(form.getClubId()));
            return "clubHandler";
        }

        try {
            tableService.update(
                    tableId,
                    form.getClubId(),
                    form.getTableNumber(),
                    form.getCapacity(),
                    TableState.DISPONIBLE,
                    form.getPrice(),
                    form.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            ra.addAttribute("err", "dup");
        } catch (NotFoundException e) {
            ra.addAttribute("err", "club_not_found");
        } catch (InvalidTableNumberException e) {
            ra.addAttribute("err", "table_number");
        } catch (InvalidCapacityException e) {
            ra.addAttribute("err", "capacity");
        } catch (InvalidPriceException e) {
            ra.addAttribute("err", "price");
        } catch (ValidationException e) {
            ra.addAttribute("err", "discount");
        } catch (IllegalStateException e) {
            ra.addAttribute("err", "max_tables");
        }
        return "redirect:/clubs/handle?clubId=" + form.getClubId();
    }


    @PostMapping("/{tableId}/delete")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String deleteTable(@PathVariable UUID tableId, @RequestParam("clubId") UUID clubId) {
        tableService.delete(tableId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/addTable")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#tableForm.clubId)")
    public String addTable(
            @Valid @ModelAttribute("tableForm") AddTableForm tableForm,
            BindingResult errors,
            Model model,
            RedirectAttributes ra
    ) {
        UUID clubId = tableForm.getClubId();

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
                            .findFirst().map(Photo::getUrl).orElse(null));
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
            var carouselPhotos = photoService.getCarouselPhotos(tableForm.getClubId());
            var tableLayout = clubLayoutService.getLayoutByClub(tableForm.getClubId());
            String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);
            model.addAttribute("carouselPhotos", carouselPhotos);
            model.addAttribute("editEventForm", editEventForm);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("tableForm", tableForm);
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            model.addAttribute("maxProductsReached", !productService.canAddProduct(clubId));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(clubId));
            return "clubHandler";
        }

        try {
            tableService.create(
                    tableForm.getTableNumber(),
                    tableForm.getCapacity(),
                    tableForm.getClubId(),
                    TableState.DISPONIBLE,
                    tableForm.getPrice(),
                    tableForm.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            ra.addAttribute("err", "dup");
        } catch (NotFoundException e) {
            ra.addAttribute("err", "club_not_found");
        } catch (InvalidTableNumberException e) {
            ra.addAttribute("err", "table_number");
        } catch (InvalidCapacityException e) {
            ra.addAttribute("err", "capacity");
        } catch (InvalidPriceException e) {
            ra.addAttribute("err", "price");
        } catch (ValidationException e) {
            ra.addAttribute("err", "discount");
        } catch (IllegalStateException e) {
            ra.addAttribute("err", "max_tables");
        }
        return "redirect:/clubs/handle?clubId=" + tableForm.getClubId();
    }

}
