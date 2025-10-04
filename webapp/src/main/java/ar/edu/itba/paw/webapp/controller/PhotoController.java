package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                         Principal principal) throws IOException {

        try {
            String userEmail = principal != null ? principal.getName() : null;
            UUID photoId = photoService.uploadPhotoForClub(clubId, userEmail, file);
            return "redirect:/clubs/" + clubId + "?photo=" + photoId;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("empty")) {
                return "redirect:/clubs/" + clubId + "?err=empty";
            } else if (e.getMessage().contains("User not found")) {
                return "redirect:/clubs/" + clubId + "?err=user";
            } else {
                return "redirect:/clubs/" + clubId + "?err=invalid";
            }
        }
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