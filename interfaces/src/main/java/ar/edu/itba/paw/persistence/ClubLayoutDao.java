package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.ClubLayout;

import java.util.Optional;
import java.util.UUID;

public interface ClubLayoutDao{
    ClubLayout save(ClubLayout layout);

    Optional<ClubLayout> findByClubId(UUID clubId);

    void deleteByClubId(UUID clubId);
}
