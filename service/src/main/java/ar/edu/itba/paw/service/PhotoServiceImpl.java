package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.PhotoDao;
import ar.edu.itba.paw.persistence.UserDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PhotoServiceImpl implements PhotoService {
    private final PhotoDao photoDao;
    private final ClubDao clubDao;
    private final UserDao userDao;

    public PhotoServiceImpl(PhotoDao photoDao, ClubDao clubDao, UserDao userDao) {
        this.photoDao = photoDao;
        this.clubDao = clubDao;
        this.userDao = userDao;
    }

    @Override
    public List<String> getPhotoUrlByClubId(UUID clubId){
        Club club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        return photoDao.findByClub(club).stream().map(Photo::getUrl).collect(Collectors.toList());
    }

    public void uploadPhoto(UUID clubId, String url, UUID userId){
        Club club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        User user = userDao.findById(userId).orElseThrow(IllegalArgumentException::new);
        photoDao.create(url, club, user);
    }
}
