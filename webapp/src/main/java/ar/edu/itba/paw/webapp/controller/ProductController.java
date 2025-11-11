package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/products")
public class ProductController {
    private final ProductService productService;
    private final TableService tableService;
    private final ClubService clubService;
    private final MenuService menuService;
    private final PhotoService photoService;
    private final EventService eventService;
    private final ClubLayoutService clubLayoutService;

    public ProductController(ProductService productService, TableService tableService, ClubService clubService, MenuService menuService, PhotoService photoService, EventService eventService, ClubLayoutService clubLayoutService) {
        this.productService = productService;
        this.tableService = tableService;
        this.clubService = clubService;
        this.menuService = menuService;
        this.photoService = photoService;
        this.eventService = eventService;
        this.clubLayoutService = clubLayoutService;
    }

    @PostMapping("/addProduct")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String addProduct(@Valid @ModelAttribute("productForm") AddProductForm form, BindingResult errors, Model model, RedirectAttributes ra) {
        UUID clubId = form.getClubId();
        if (errors.hasErrors()) {
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(clubId));
            model.addAttribute("menu", menuService.getMenuByClub(clubId));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", form);
            model.addAttribute("editProductForm", new EditProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            EventAddForm eventForm = new EventAddForm();
            eventForm.setClubId(club.getClubId());
            model.addAttribute("eventForm", eventForm);
            EventEditForm editEventForm = new EventEditForm();
            editEventForm.setClubId(club.getClubId());
            model.addAttribute("editEventForm", editEventForm);
            model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(club));
            return "clubHandler";
        }
        if (!productService.canAddProduct(clubService.getClubById(clubId).orElseThrow())) {
            ra.addAttribute("err", "max_products");
            return "redirect:/clubs/handle?clubId=" + clubId;
        }
        productService.create(clubId, form.getProductName(), form.getDescription(), form.getBasePrice(), form.getProductType(), form.getIsAvailable());
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/deleteProduct")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String deleteProduct(@RequestParam("productId") UUID productId,
                                @RequestParam("clubId") UUID clubId) {
        productService.delete(productId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/editProduct")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String editProduct(@Valid @ModelAttribute("editProductForm") EditProductForm form, BindingResult errors, RedirectAttributes ra, Model model) {
        UUID clubId = form.getClubId();
        if (errors.hasErrors()) {
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(clubId));
            model.addAttribute("menu", menuService.getMenuByClub(clubId));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
            ClubHandlerForm clubHandlerForm = new ClubHandlerForm();
            clubHandlerForm.setClubId(club.getClubId());
            clubHandlerForm.setName(club.getClubName());
            clubHandlerForm.setDescription(club.getDescription());
            clubHandlerForm.setLocation(club.getLocation());
            clubHandlerForm.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
            clubHandlerForm.setZone(club.getZone() != null ? club.getZone().name() : null);
            clubHandlerForm.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
            model.addAttribute("clubHandlerForm", clubHandlerForm);
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", new AddProductForm());
            model.addAttribute("editTableForm", new EditTableForm());
            model.addAttribute("editProductForm", form);
            model.addAttribute("editField", "product");
            EventAddForm eventForm = new EventAddForm();
            eventForm.setClubId(club.getClubId());
            model.addAttribute("eventForm", eventForm);
            EventEditForm editEventForm = new EventEditForm();
            editEventForm.setClubId(club.getClubId());
            model.addAttribute("errors", errors);
            model.addAttribute("editEventForm", editEventForm);
            model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
            var carouselPhotos = photoService.getCarouselPhotos(clubId, 10, 0);
            var tableLayout = clubLayoutService.getLayoutByClub(clubId);
            String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);
            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
            model.addAttribute("carouselPhotos", carouselPhotos);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);
            model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
            model.addAttribute("maxTablesReached", !tableService.canAddTable(club));
            return "clubHandler";
        }
        try {
            productService.update(form.getProductId(), clubId, form.getProductName(), form.getDescription(), form.getBasePrice(), form.getProductType(), form.getIsAvailable());
        } catch (ValidationException e) {
            ra.addAttribute("err", "price_max_product");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("err", "name");
        }
        return "redirect:/clubs/handle?clubId=" + clubId;
    }
}