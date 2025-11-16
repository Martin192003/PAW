package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.util.*;

@Controller
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final PhotoService photoService;

    public ReviewController(ReviewService reviewService, UserService userService, PhotoService photoService) {
        this.photoService = photoService;
        this.reviewService = reviewService;
        this.userService = userService;
    }


    @GetMapping("/reviews/clubs/{clubId}/user")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public Review getUserReview(@PathVariable UUID clubId, @AuthenticationPrincipal PawUserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }

        return reviewService.getUserReviewForClub(userDetails.getId(), clubId).orElse(null);
    }

    @PostMapping("/clubs/{clubId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public String submitReview(@PathVariable UUID clubId,
                               @Valid @ModelAttribute("reviewForm") ReviewForm form,
                               BindingResult br,
                               RedirectAttributes ra,
                               @RequestParam(required = false, name = "photo") MultipartFile photo, @AuthenticationPrincipal PawUserDetails userDetails) throws IOException {

        if (br.hasErrors()) {
            ra.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm", br);
            ra.addFlashAttribute("reviewForm", form);
            return "redirect:/clubs/" + clubId + "#reviews";
        }

        Photo image = null;
        if (photo != null && !photo.isEmpty()) {
            image = photoService.createPhoto(
                    photo.getOriginalFilename(),
                    photo.getContentType(),
                    photo.getBytes()
            );
        }

        reviewService.createReview(userDetails.getId(), clubId, form.getRating(), form.getDescription(), image);
        return "redirect:/clubs/" + clubId + "#reviews";
    }

    @GetMapping("/users/{userId}/reviews")
    public String listUserReviews(@PathVariable UUID userId,
                                  @RequestParam(defaultValue = "created_desc") String sort,
                                  @RequestParam(defaultValue = "10") int limit,
                                  @RequestParam(defaultValue = "0") int offset,
                                  Model model) {


        final User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("error.user.notfound"));

        final int total = reviewService.countByUser(userId);

        limit = Math.max(1, Math.min(limit, 50));
        offset = Math.max(0, Math.min(offset, limit));

        int calculateOffset = reviewService.calculateOffset(offset, limit, total);
        final List<Review> pageReviews = reviewService.obtainPaginatedReviews(userId, sort, limit, offset);


        model.addAttribute("user", user);
        model.addAttribute("reviews", pageReviews);
        model.addAttribute("total", total);
        model.addAttribute("limit", limit);
        model.addAttribute("offset", calculateOffset);
        model.addAttribute("sort", sort);


        model.addAttribute("currentPage", (calculateOffset / limit) + 1);
        model.addAttribute("totalPages", (int) Math.ceil(total / (double) limit));


        return "userReviews";
    }

   @PostMapping("/reviews/{reviewId}/delete")
   @PreAuthorize("isAuthenticated()")
   public String deleteReview(@PathVariable UUID reviewId,
                              @RequestHeader(value = "referer", required = false) String referer,
                              RedirectAttributes ra) {
       try {
           reviewService.deleteReview(reviewId);
           ra.addFlashAttribute("success", "review.deleted");
       } catch (Exception e) {
           ra.addFlashAttribute("error", "review.notdeleted");
       }

       if (referer != null && !referer.isBlank()) {
           return "redirect:" + referer;
       }
       return "redirect:/";
   }

}