package ar.edu.itba.paw.service;

import java.util.List;
import java.util.UUID;

public interface PhotoService {
    List<String> getPhotoUrlByClubId(UUID clubId);

    void uploadPhoto(UUID clubId, String url, UUID userId);
}
