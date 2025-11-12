package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.component.ClubHandlerModelBuilder;
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
    private final ClubHandlerModelBuilder modelBuilder;


    public ProductController(ProductService productService, ClubHandlerModelBuilder modelBuilder) {
        this.productService = productService;
        this.modelBuilder = modelBuilder;
    }

    @PostMapping("/addProduct")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String addProduct(@Valid @ModelAttribute("productForm") AddProductForm form, BindingResult errors, Model model, RedirectAttributes ra) {
        UUID clubId = form.getClubId();
        if (errors.hasErrors()) {
            modelBuilder.populateForAdd(model, clubId, form);
            return "clubHandler";
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
            model.addAttribute(BindingResult.MODEL_KEY_PREFIX + "editProductForm", errors);
            modelBuilder.populateForEditErrors(model, clubId, form);
            model.addAttribute("openModal", "editProduct");
            return "clubHandler";
        }
        productService.update(form.getProductId(), clubId, form.getProductName(), form.getDescription(), form.getBasePrice(), form.getProductType(), form.getIsAvailable());
        return "redirect:/clubs/handle?clubId=" + clubId;
    }
}