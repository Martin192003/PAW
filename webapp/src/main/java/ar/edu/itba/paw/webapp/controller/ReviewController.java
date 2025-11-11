package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.UserNotFoundException;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.ReviewForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    public ReviewController(ReviewService reviewService, UserService userService) {
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

        //TODO: FIJARSE SI ESTA BIEN ESTO
        Photo image = new Photo();
        image.setFilename(photo.getOriginalFilename());
        image.setContentType(photo.getContentType());
        image.setData(photo.getBytes());

        reviewService.createReview(userDetails.getId(), clubId, form.getRating(), form.getDescription(), image);
        return "redirect:/clubs/" + clubId + "#reviews";
    }

    @GetMapping("/users/{userId}/reviews")
    public String listUserReviews(@PathVariable UUID userId,
                                  @RequestParam(defaultValue = "created_desc") String sort,
                                  @RequestParam(defaultValue = "10") int limit,
                                  @RequestParam(defaultValue = "0") int offset,
                                  Model model) {


        // Normalizar parámetros
        limit = Math.max(1, Math.min(limit, 50));
        offset = Math.max(0, offset);


        // Traer el usuario solo para mostrar nombre/validar existencia
        final User user = userService.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("error.user.notfound"));


        // Datos paginados
        final List<Review> reviews = reviewService.listByUser(userId, sort, limit, offset);
        final int total = reviewService.countByUser(userId);


        // Ajuste si el offset quedó fuera de rango (p. ej., se borraron reviews)
        if (offset >= total && total > 0) {
            int lastPage = (int) Math.floor((total - 1) / (double) limit);
            offset = lastPage * limit;
        }


        // Si recalculamos offset, recargar la página
        final List<Review> pageReviews = (offset == 0 || offset <= total)
                ? reviewService.listByUser(userId, sort, limit, offset)
                : reviews;


        model.addAttribute("user", user);
        model.addAttribute("reviews", pageReviews);
        model.addAttribute("total", total);
        model.addAttribute("limit", limit);
        model.addAttribute("offset", offset);
        model.addAttribute("sort", sort);


        // Para UI (paginación)
        model.addAttribute("currentPage", (offset / limit) + 1);
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