package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubAdminDao {
    boolean exists(Club club, User admin);
    List<User> findAdmins(Club club);
    List<Club> findClubsByAdmin(User admin);

    boolean addAdmin(Club club, User admin);
    boolean removeAdmin(Club club, User admin);
}
