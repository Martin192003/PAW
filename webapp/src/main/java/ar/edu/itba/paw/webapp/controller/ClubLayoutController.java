package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

@Controller
public class ClubLayoutController {

    private final ClubLayoutService clubLayoutService;

    @Autowired
    public ClubLayoutController(ClubLayoutService clubLayoutService) {
        this.clubLayoutService = clubLayoutService;
    }


    @PostMapping("/clubs/club/{clubId}/layout")
    @PreAuthorize("isAuthenticated()")
    public String uploadLayout(@PathVariable UUID clubId,
                               @RequestParam("file") MultipartFile file,
                               RedirectAttributes ra, @AuthenticationPrincipal PawUserDetails userDetails) throws IOException {
        clubLayoutService.uploadLayout(
                clubId,
                userDetails.getUser().getEmail(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
        ra.addFlashAttribute("layoutUploaded", true);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @GetMapping("/clubs/club/{clubId}/layout/{layoutId}/image")
    public ResponseEntity<byte[]> getLayoutImage(
            @PathVariable UUID clubId) {

        var layoutOpt = clubLayoutService.getLayoutByClub(clubId);

        if (layoutOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClubLayout layout = layoutOpt.get();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(layout.getContentType()))
                .body(layout.getData());
    }
}
