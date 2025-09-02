package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.service.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
public class PhotoController {
    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping("/club/{clubId}")
    public List<String> getPhotoByClubId(@PathVariable UUID clubId){
        return photoService.getPhotoUrlByClubId(clubId);
    }

    @PostMapping("/club/{clubId}")
    public void uploadPhoto(@PathVariable UUID clubId, @RequestParam String url, @RequestParam UUID userId){
        photoService.uploadPhoto(clubId, url, userId);
    }

}
