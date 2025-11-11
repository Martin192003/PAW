package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.User;

import java.util.Optional;
import java.util.UUID;

public interface SecurityService {
    Optional<User> getLoggedUser();
    Optional<String> getLoggedUserEmail();
    User getVerifiedLoggedUser();
    public boolean isClubAdmin(UUID clubId);
    public boolean canAccessUser(UUID id);
}
