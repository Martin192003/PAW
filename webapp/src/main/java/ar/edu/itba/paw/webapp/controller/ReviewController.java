package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Review;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.UUID;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final SecurityService securityService;
    private final PhotoService photoService;

    public ReviewController(ReviewService reviewService, UserService userService, SecurityService securityService, PhotoService photoService) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.securityService = securityService;
        this.photoService = photoService;
    }


    @PostMapping("clubs/reviews/clubs/{clubId}")
    public String createReview(@PathVariable UUID clubId,
                                     @RequestParam int rating,
                                     @RequestParam(value = "photo", required = false) MultipartFile photo,
                                     @RequestParam(required = false) String description,
                                     RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "redirect:/login";
        }

        String username = auth.getName();
        User user = userService.findByEmail(username);
        if (user == null) {
            return "redirect:/login";
        }



        try {
            Review review = reviewService.createReview(user.getId(), clubId, rating, description);
            if (photo != null && !photo.isEmpty()) {
                photoService.uploadReviewPhoto(clubId, review.getReviewId(), user.getId(), photo);
            }
            return "redirect:/clubs/" + clubId;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clubs/" + clubId ;
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error subiendo la imagen.");
            return "redirect:/clubs/" + clubId ;
        }

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

    @PostMapping("/clubs/{clubId}/reviews")
    public String submitReview(@PathVariable UUID clubId,
                               @RequestParam int rating,
                               @RequestParam String comment,
                               Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = securityService.getVerifiedLoggedUser();
        reviewService.createReview(user.getId(), clubId, rating, comment);
        return "redirect:/clubs/" + clubId;
    }



}