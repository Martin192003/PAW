package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final SecurityService securityService;
    private final PhotoService photoService;
    private final ClubService clubService;
    private final ProductService productsService;
    private final ClubLayoutService clubLayoutService;

    public ReviewController(ReviewService reviewService, UserService userService, SecurityService securityService, PhotoService photoService, ClubService clubService, ProductService productsService, ClubLayoutService clubLayoutService) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.securityService = securityService;
        this.photoService = photoService;
        this.clubService = clubService;
        this.productsService = productsService;
        this.clubLayoutService = clubLayoutService;
    }


    @GetMapping("/reviews/clubs/{clubId}/user")
    @ResponseBody
    public Review getUserReview(@PathVariable UUID clubId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        String username = auth.getName();
        User user = userService.findByEmail(username);
        if (user == null) {
            return null;
        }

        return reviewService.getUserReviewForClub(user.getId(), clubId).orElse(null);
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    @PostMapping("/clubs/{clubId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public String submitReview(@PathVariable UUID clubId,
                               @Valid @ModelAttribute("reviewForm") ReviewForm form,
                               BindingResult br,
                               Model model,
                               RedirectAttributes ra,
                               @RequestParam(required = false, name = "photo") MultipartFile photo) throws IOException {

        if (br.hasErrors()) {
            Club club = clubService.getClubById(clubId)
                    .orElseThrow(() -> new IllegalArgumentException("Club not found"));

            model.addAttribute("club", club);
            model.addAttribute("reviewForm", form); // Mantener el formulario con los datos ingresados
            model.addAttribute("reviews", reviewService.getReviewsByClub(clubId));
            model.addAttribute("isLoggedIn", securityService.getVerifiedLoggedUser() != null);

            List<Photo> carouselPhotos = photoService.getClubPhotos(clubId, 10, 0);
            model.addAttribute("clubPhotos", carouselPhotos);

            List<Photo> reviewPhotos = photoService.getReviewPhotosByClub(clubId, 20, 0);
            model.addAttribute("reviewPhotos", reviewPhotos);

            var products = productsService.findByClub(club).stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsAvailable()))
                    .sorted(Comparator.comparing(Product::getProductName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            var grouped = products.stream().collect(Collectors.groupingBy(productsService::classifyCategory));
            var desired = List.of("Cervezas", "Tragos", "Champagne", "Sin Alcohol");
            var ordered = new LinkedHashMap<String, List<Product>>();
            desired.forEach(cat -> {
                var list = grouped.remove(cat);
                if (list != null && !list.isEmpty()) ordered.put(cat, list);
            });
            grouped.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                    .forEach(e -> ordered.put(e.getKey(), e.getValue()));
            if (ordered.isEmpty() && !products.isEmpty()) ordered.put("Tragos", products);
            model.addAttribute("productsByCategory", ordered);

            User user = securityService.getVerifiedLoggedUser();
            if (user != null) {
                model.addAttribute("user", user);
                model.addAttribute("userReview", reviewService.getUserReviewForClub(user.getId(), clubId).orElse(null));
                model.addAttribute("isAuthenticated", true);
                boolean isOwner = clubService.getAdminsByClub(clubId).stream().anyMatch(u -> u.getId().equals(user.getId()));
                model.addAttribute("isOwner", isOwner);
            } else {
                model.addAttribute("isAuthenticated", false);
                model.addAttribute("isOwner", false);
            }

            String tableLayoutUrl = clubLayoutService.getLayoutByClub(clubId)
                    .map(ClubLayout::getUrl)
                    .orElse(null);
            model.addAttribute("tableLayoutUrl", tableLayoutUrl);

            model.addAttribute("locationText", club.getLocation() != null ? club.getLocation() : "Sin ubicación");
            model.addAttribute("latitude", -34.5891429);
            model.addAttribute("longitude", -58.3961641);

            return "clubDetails";
        }

        User user = securityService.getVerifiedLoggedUser();
        reviewService.createReview(user.getId(), clubId, form.getRating(), form.getDescription(), photo);
        return "redirect:/clubs/" + clubId + "#reviews";
    }
}