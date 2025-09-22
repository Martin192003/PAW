package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.exceptions.DuplicateEntityException;
import ar.edu.itba.paw.model.exceptions.TableNotFoundException;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
import ar.edu.itba.paw.persistence.ReviewsDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
    @Transactional(readOnly = true)
    public Optional<Table> findById(UUID id) {
        return clubTablesDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findByClubId(UUID clubId) {
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        var list = clubTablesDao.findByClub(club);
        return list;
    }
    @Override
    @Transactional(readOnly = true)
    public List<Table> findAll() {
        return clubTablesDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Table> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to) {
        // valida club y normaliza rango
        var club = clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new);
        if (!from.isBefore(to)) throw new IllegalArgumentException("Rango inválido");
        return clubTablesDao.findAvailableByClubAndRange(club.getClubId(), from, to);
    }

    @Override
    @Transactional
    public Table create(int number, int capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount) {
        if(!clubDao.findById(clubId).isPresent()) throw new IllegalArgumentException("Club no existente");
        if(number <= 0 || capacity <= 0 || price.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Parámetros inválidos");
        if(clubTablesDao.findByClub(clubDao.findById(clubId).get()).stream().anyMatch(t -> t.getTableNumber() == number))
            throw new DuplicateEntityException("Número de mesa ya existente en el club");
        if(price.compareTo(BigDecimal.valueOf(1000000000)) >= 0)
            throw new IllegalArgumentException("Precio demasiado alto");
        return clubTablesDao.create(number, capacity, clubId, tableState, price, discount);
    }

    @Override
    @Transactional
    public Table update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity, TableState state, BigDecimal price, BigDecimal discount) {
        return clubTablesDao.update(tableId, clubId, tableNumber, capacity, state, price, discount);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        return clubTablesDao.delete(id);
    }
}
