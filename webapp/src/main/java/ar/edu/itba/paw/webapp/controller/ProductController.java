package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.MusicalGenre;
import ar.edu.itba.paw.model.Price;
import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.Zone;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.model.exceptions.ValidationException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.AddProductForm;
import ar.edu.itba.paw.webapp.form.AddTableForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/products")
public class ProductController {

    private final ProductService productService;
    private final SecurityService securityService;
    private final TableService tableService;
    private final ClubService clubService;
    private final MenuService menuService;
    private final PhotoService photoService;

    public ProductController(ProductService productService, SecurityService securityService, TableService tableService, ClubService clubService, MenuService menuService, PhotoService photoService) {
        this.productService = productService;
        this.securityService = securityService;
        this.tableService = tableService;
        this.clubService = clubService;
        this.menuService = menuService;
        this.photoService = photoService;
    }

    @PostMapping("/addProduct")
    public String addProduct(@Valid @ModelAttribute("productForm") AddProductForm form,
                             BindingResult errors, Model model) {
        UUID clubId = form.getClubId();

        var user = securityService.getVerifiedLoggedUser();
        if (user == null) throw new UserNotFoundException("No logueado");
        clubService.assertAdmin(clubId, user.getId());

        if (errors.hasErrors()) {
            var club = clubService.getClubById(clubId).orElseThrow();
            model.addAttribute("club", club);
            model.addAttribute("tables", tableService.findByClubId(clubId));
            model.addAttribute("menu", menuService.getMenuByClub(clubId));
            model.addAttribute("zones", Zone.values());
            model.addAttribute("genres", MusicalGenre.values());
            model.addAttribute("prices", Price.values());
            model.addAttribute("coverPhotoUrl", photoService.getCoverUrl(clubId).orElse(null));
            model.addAttribute("tableForm", new AddTableForm());
            model.addAttribute("productForm", form);
            return "clubHandler";
        }

        var club = clubService.getClubById(clubId).orElseThrow();
        productService.create(club, form.getProductName(), form.getDescription(),
                form.getBasePrice(), form.getProductType(), form.getIsAvailable());
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/deleteProduct")
    public String deleteProduct(@RequestParam("productId") UUID productId) {
        var product = productService.findById(productId).orElseThrow();
        UUID clubId = product.getClub().getClubId();

        var user = securityService.getVerifiedLoggedUser();
        if (user == null) throw new UserNotFoundException("No logueado");
        clubService.assertAdmin(clubId, user.getId());

        productService.delete(productId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/editProduct")
    public String editProduct(@RequestParam("productId") UUID productId,
                              @RequestParam("productName") String productName,
                              @RequestParam("description") String description,
                              @RequestParam("basePrice") BigDecimal basePrice,
                              @RequestParam("productType") String productType,
                              RedirectAttributes ra) {

        var product = productService.findById(productId).orElseThrow();
        UUID clubId = product.getClub().getClubId();

        var user = securityService.getVerifiedLoggedUser();
        if (user == null) throw new UserNotFoundException("No logueado");
        clubService.assertAdmin(clubId, user.getId());

        try {
            productService.update(productId, clubId, productName, description, basePrice, ProductType.fromDb(productType), product.getIsAvailable());
        } catch (ValidationException e) {
            ra.addAttribute("err", "price_max_product");
        } catch (IllegalArgumentException e) {
            ra.addAttribute("err", "name");
        }
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

}
