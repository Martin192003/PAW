package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.Photo;

import java.util.Optional;
import java.util.UUID;

public interface ClubLayoutService {
    void uploadLayout(UUID clubId, String userEmail, String filename, String contentType, byte[] data);
    Optional<ClubLayout> getLayoutByClub(UUID clubId);
}
