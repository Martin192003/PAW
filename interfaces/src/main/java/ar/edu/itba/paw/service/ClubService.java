package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import java.util.List;
import java.util.UUID;

public interface ClubService {
    List<Club> getAllClubs();
    Club getClubById(UUID clubId);
}