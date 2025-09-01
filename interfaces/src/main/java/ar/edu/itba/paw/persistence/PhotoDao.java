package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoDao {
    Optional<Photo> findById(UUID id);
    List<Photo> findByClub(Club club);
    List<Photo> findByUser(User user);

    Photo create(String url, Club club, User postedBy);
    Photo update(UUID photoId, String url);
    boolean delete(UUID id);
}
