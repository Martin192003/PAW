package ar.edu.itba.paw.webapp.component;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.UUID;

@Component
public class ClubHandlerModelBuilder {

    private final ClubService clubService;
    private final TableService tableService;
    private final MenuService menuService;
    private final PhotoService photoService;
    private final ProductService productService;
    private final EventService eventService;
    private final ClubLayoutService clubLayoutService;

    public ClubHandlerModelBuilder(ClubService clubService,
                                   TableService tableService,
                                   MenuService menuService,
                                   PhotoService photoService,
                                   ProductService productService,
                                   EventService eventService,
                                   ClubLayoutService clubLayoutService) {
        this.clubService = clubService;
        this.tableService = tableService;
        this.menuService = menuService;
        this.photoService = photoService;
        this.productService = productService;
        this.eventService = eventService;
        this.clubLayoutService = clubLayoutService;
    }


    public void populateForAdd(Model model, UUID clubId, AddProductForm form) {
        var club = getClub(clubId);
        addBaseAttributes(model, club);
        addFormsForAdd(model, club, form);
        addLimits(model, club);
    }

    public void populateForEditErrors(Model model, UUID clubId, EditProductForm form) {
        var club = getClub(clubId);
        addBaseAttributes(model, club);
        addFormsForEdit(model, club, form);
        addEditExtras(model, clubId);
        addLimits(model, club);
    }


    private Club getClub(UUID clubId) {
        return clubService.getClubById(clubId).orElseThrow();
    }

    private void addBaseAttributes(Model model, Club club) {
        UUID clubId = club.getClubId();
        model.addAttribute("club", club);
        model.addAttribute("tables", tableService.findByClubId(clubId));
        model.addAttribute("menu", menuService.getMenuByClub(clubId));
        model.addAttribute("zones", Zone.values());
        model.addAttribute("genres", MusicalGenre.values());
        model.addAttribute("prices", Price.values());
        model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
    }

    private void addFormsForAdd(Model model, Club club, AddProductForm productForm) {
        model.addAttribute("clubHandlerForm", toClubHandlerForm(club));
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("productForm", productForm);
        model.addAttribute("editProductForm", new EditProductForm());
        model.addAttribute("editTableForm", new EditTableForm());

        EventAddForm eventForm = new EventAddForm();
        eventForm.setClubId(club.getClubId());
        model.addAttribute("eventForm", eventForm);

        EventEditForm editEventForm = new EventEditForm();
        editEventForm.setClubId(club.getClubId());
        model.addAttribute("editEventForm", editEventForm);
    }

    private void addFormsForEdit(Model model, Club club, EditProductForm editProductForm) {
        model.addAttribute("clubHandlerForm", toClubHandlerForm(club));
        model.addAttribute("tableForm", new AddTableForm());
        model.addAttribute("productForm", new AddProductForm());
        model.addAttribute("editTableForm", new EditTableForm());
        model.addAttribute("editProductForm", editProductForm);
        model.addAttribute("editField", "product");
        EventAddForm eventForm = new EventAddForm();
        eventForm.setClubId(club.getClubId());
        model.addAttribute("eventForm", eventForm);

        EventEditForm editEventForm = new EventEditForm();
        editEventForm.setClubId(club.getClubId());
        model.addAttribute("editEventForm", editEventForm);
    }

    private void addEditExtras(Model model, UUID clubId) {
        model.addAttribute("events", eventService.listByClub(clubId, 0, 20));
        var carouselPhotos = photoService.getCarouselPhotos(clubId, 10, 0);
        var tableLayout = clubLayoutService.getLayoutByClub(clubId);
        String tableLayoutUrl = tableLayout.map(ClubLayout::getUrl).orElse(null);
        model.addAttribute("carouselPhotos", carouselPhotos);
        model.addAttribute("tableLayoutUrl", tableLayoutUrl);
    }

    private void addLimits(Model model, Club club) {
        model.addAttribute("maxProductsReached", !productService.canAddProduct(club));
        model.addAttribute("maxTablesReached", !tableService.canAddTable(club));
    }

    private ClubHandlerForm toClubHandlerForm(Club club) {
        ClubHandlerForm f = new ClubHandlerForm();
        f.setClubId(club.getClubId());
        f.setName(club.getClubName());
        f.setDescription(club.getDescription());
        f.setLocation(club.getLocation());
        f.setPriceRange(club.getPriceRange() != null ? club.getPriceRange().name() : null);
        f.setZone(club.getZone() != null ? club.getZone().name() : null);
        f.setMusicGenre(club.getMusicGenre() != null ? club.getMusicGenre().name() : null);
        return f;
    }
}
