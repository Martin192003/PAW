package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.*;

import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.form.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
public class PhotoController {

    private static final Logger log = LoggerFactory.getLogger(PhotoController.class);

    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;

    }

    @PostMapping("/clubs/{clubId}/photos")
    public String upload(@PathVariable UUID clubId,
                         @RequestParam("file") MultipartFile file,
                         Principal principal,
                         RedirectAttributes redirectAttributes) throws IOException {
        String userEmail = principal != null ? principal.getName() : null;
        Photo photo = photoService.createPhoto(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
        photoService.uploadPhotoForClub(clubId, userEmail, photo);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @GetMapping("/photos/{photoId}")
    @ResponseBody
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID photoId) {
        Optional<Photo> opt = photoService.getWithId(photoId);
        if (opt.isEmpty()) {
            log.warn("Photo {} not found", photoId);
            return ResponseEntity.notFound().build();
        }

        Photo photo = opt.get();
        if (photo.getData() == null || photo.getData().length == 0) {
            log.warn("Photo {} has no data", photoId);
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = sniff(photo.getData());
        log.info("Serving photo {} ({} bytes, {})", photoId, photo.getData().length, mediaType);
        return ResponseEntity.ok().contentType(mediaType).body(photo.getData());
    }

    @PostMapping("/clubs/club/{clubId}/gallery")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String addPhotosToGallery(@PathVariable UUID clubId,
                                     @RequestParam("files") MultipartFile[] files,
                                     RedirectAttributes redirectAttributes,
                                     @AuthenticationPrincipal PawUserDetails userDetails) {
        int uploaded = 0;

        try {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    photoService.uploadCarousel(
                            clubId,
                            userDetails.getId(),
                            file.getOriginalFilename(),
                            file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                            file.getBytes()
                    );
                    uploaded++;
                }
            }

            if (uploaded > 0) {
                redirectAttributes.addFlashAttribute("success", "photos.added.successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "photos.upload.error");
            }

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", "photos.upload.max");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "photos.upload.error");
        }
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/clubs/club/{clubId}/cover")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String uploadCover(@PathVariable UUID clubId,
                              @RequestParam("file") MultipartFile file, @AuthenticationPrincipal PawUserDetails userDetails) throws IOException {

        if (file == null || file.isEmpty()) {
            return "redirect:/clubs/handle?clubId=" + clubId + "&err=empty";
        }

        photoService.uploadCover(
                clubId,
                userDetails.getId(),
                file.getOriginalFilename(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getBytes()
        );

        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/clubs/club/{clubId}/photos")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String upload(@PathVariable UUID clubId,
                         @RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "redirect:/clubs/handle?clubId=" + clubId + "&err=empty";
        }
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/clubs/{clubId}/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable UUID clubId,
                              @PathVariable UUID photoId,
                              RedirectAttributes redirectAttributes) {


        boolean deleted = photoService.delete(photoId);

        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "photo.deleted.success");
        } else {
            redirectAttributes.addFlashAttribute("error", "photo.delete.error");
        }

        return "redirect:/clubs/handle?clubId=" + clubId;
    }



    private static MediaType sniff(byte[] d) {
        if (d.length >= 4 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (d.length >= 2 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8) {
            return MediaType.IMAGE_JPEG;
        }
        if (d.length >= 12) {
            String fourCC = new String(d, 8, 4, StandardCharsets.US_ASCII);
            if ("WEBP".equals(fourCC)) return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }



}