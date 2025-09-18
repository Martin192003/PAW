package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TableServiceImpl implements TableService {


    private final ClubDao clubDao;
    private final ClubTablesDao clubTablesDao;

    public TableServiceImpl(ClubTablesDao clubTablesDao, ClubDao clubDao) {
        this.clubTablesDao = clubTablesDao;
        this.clubDao = clubDao;
    }


    @Override
    public Optional<Table> findById(UUID id) {
        return clubTablesDao.findById(id);
    }

    @Override
    public List<Table> findByClubId(UUID clubId) {
        return clubTablesDao.findByClub(clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new));
    }

    @Override
    public List<Table> findAll() {
        return clubTablesDao.findAll();
    }

    @Override
    public Table create(int number, int capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount) {
        return clubTablesDao.create(number, capacity, clubId, tableState, price, discount);
    }

    @Override
    public Table update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity, TableState state, BigDecimal price, BigDecimal discount) {
        return clubTablesDao.update(tableId, clubId, tableNumber, capacity, state, price, discount);
    }

    @Override
    public boolean delete(UUID id) {
        return clubTablesDao.delete(id);
    }
}
