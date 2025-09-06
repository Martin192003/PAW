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
    private final UserService userService;

    public PhotoController(PhotoService photoService, UserService userService) {
        this.photoService = photoService;
        this.userService = userService;
    }

    @PostMapping("/clubs/{clubId}/photos")
    public String upload(@PathVariable UUID clubId,
                         @RequestParam("file") MultipartFile file,
                         Principal principal) throws IOException {

        if (file == null || file.isEmpty()) {
            return "redirect:/clubs/" + clubId + "?err=empty";
        }

        var effectiveUser = (principal != null)
                ? userService.findByEmail(principal.getName())
                : userService.findByEmail("dev@local");

        if (effectiveUser == null) {
            return "redirect:/clubs/" + clubId + "?err=user";
        }

        UUID photoId = photoService.upload(
                clubId,
                effectiveUser.getId(),
                file.getOriginalFilename(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                file.getBytes()
        );

        return "redirect:/clubs/" + clubId + "?photo=" + photoId;
    }


    @GetMapping("/photos/{photoId}")
    @ResponseBody
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID photoId) {
        var opt = photoService.getWithId(photoId);
        if (opt.isEmpty()) {
            log.warn("Photo {} not found", photoId);
            return ResponseEntity.notFound().build();
        }
        var p = opt.get();
        if (p.getData() == null || p.getData().length == 0) {
            log.warn("Photo {} has no data", photoId);
            return ResponseEntity.notFound().build();
        }
        var mt = sniff(p.getData());
        log.info("Serving photo {} ({} bytes, {})", photoId, p.getData().length, mt);
        return ResponseEntity.ok().contentType(mt).body(p.getData());
    }
    private static MediaType sniff(byte[] d) {
        if (d.length >= 4
                && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if (d.length >= 2
                && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8) { // JPEG SOI
            return MediaType.IMAGE_JPEG;
        }
        if (d.length >= 12) {
            String fourCC = new String(d, 8, 4, StandardCharsets.US_ASCII);
            if ("WEBP".equals(fourCC)) return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}