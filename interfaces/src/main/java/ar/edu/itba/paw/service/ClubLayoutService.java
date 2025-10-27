package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.ClubLayout;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

public interface ClubLayoutService {
    UUID uploadLayout(UUID clubId, String userEmail, MultipartFile file);
    Optional<ClubLayout> getLayoutByClub(UUID clubId);
    boolean deleteLayout(UUID clubId);
}
