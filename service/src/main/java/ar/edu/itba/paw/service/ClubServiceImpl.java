package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.persistence.ClubDao;

import java.util.List;
import java.util.UUID;

public class ClubServiceImpl implements ClubService{

    private final ClubDao clubDao;

    public ClubServiceImpl(ClubDao clubDao) {
        this.clubDao = clubDao;
    }

    @Override
    public List<Club> getAllClubs() {
        return clubDao.findAll();

    }

    @Override
    public Club getClubById(UUID clubId) {
        return clubDao.findById(clubId);
    }
}
