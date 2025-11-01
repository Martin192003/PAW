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
                               RedirectAttributes ra,
                               @RequestParam(required = false, name = "photo") MultipartFile photo) throws IOException {

        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm", br);
            ra.addFlashAttribute("reviewForm", form);
            return "redirect:/clubs/" + clubId + "#reviews";
        }

        User user = securityService.getVerifiedLoggedUser();
        reviewService.createReview(user.getId(), clubId, form.getRating(), form.getDescription(), photo);
        return "redirect:/clubs/" + clubId + "#reviews";
    }


}