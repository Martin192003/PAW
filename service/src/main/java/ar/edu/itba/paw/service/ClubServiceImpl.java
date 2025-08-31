package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.persistence.ClubDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;
    @Autowired
    public ClubServiceImpl(ClubDao clubDao) {
        this.clubDao = clubDao;
    }

    @Override
    public List<Club> getAllClubs() {
        return clubDao.findAll();

    }

    @Override
    public Optional<Club> getClubById(UUID clubId) {
        return Optional.ofNullable(clubDao.findById(clubId));
    }
}
