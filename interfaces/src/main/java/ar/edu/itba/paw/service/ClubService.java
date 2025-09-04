package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();
    Optional<Club> getClubById(UUID clubId);
}