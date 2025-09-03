package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.service.UserService;
import ar.edu.itba.paw.model.Photo;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
public class PhotoController {

    private final PhotoService photoService;
    private final UserService userService;

    public PhotoController(PhotoService photoService, UserService userService) {
        this.photoService = photoService;
        this.userService = userService;
    }


    @GetMapping("/clubs/{clubId}/photos")
    public ModelAndView listClubPhotos(@PathVariable UUID clubId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        List<Photo> photos = photoService.listByClub(clubId, page, size);
        ModelAndView mav = new ModelAndView("clubPhotos"); // JSP: /WEB-INF/jsp/clubPhotos.jsp
        mav.addObject("clubId", clubId);
        mav.addObject("photos", photos);
        mav.addObject("page", page);
        mav.addObject("size", size);
        return mav;
    }

    @PostMapping("/clubs/{clubId}/photos")
    public String upload(@PathVariable UUID clubId,
                         @RequestParam("file") MultipartFile file,
                         Principal principal) throws IOException {

        var user = userService.findByEmail(principal.getName());

        Photo p = new Photo();
        p.setClubId(clubId);
        p.setPostedBy(user.getId());
        p.setFilename(file.getOriginalFilename());
        p.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        p.setSizeBytes(file.getSize());
        p.setData(file.getBytes()); // <--- byte[] para BYTEA
        p.setUrl("/photos/" + UUID.randomUUID()); // o la ruta que uses para servirla

        photoService.save(p); // inserta

        return "redirect:/clubs/" + clubId;
    }


}