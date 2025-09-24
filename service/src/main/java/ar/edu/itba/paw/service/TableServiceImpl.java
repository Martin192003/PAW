package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ClubTablesDao;
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
    public Table create(BigDecimal number, BigDecimal capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( number == null || number.compareTo(BigDecimal.ZERO) < 0 || number.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        return clubTablesDao.create(number.intValueExact(), capacity.intValueExact(), clubId, tableState, price, discount);
    }

    @Override
    @Transactional
    public Table update(UUID tableId, UUID clubId, BigDecimal tableNumber, BigDecimal capacity, TableState state, BigDecimal price, BigDecimal discount) {
        if (clubDao.findById(clubId).isEmpty()) throw new NotFoundException("Club no existente");
        if ( tableNumber == null || tableNumber.compareTo(BigDecimal.ZERO) < 0 || tableNumber.compareTo(BigDecimal.valueOf(1_000_000)) >=  0) throw new InvalidTableNumberException("Número de mesa inválido");
        if ( capacity == null || capacity.compareTo(BigDecimal.ZERO) <= 0 || capacity.compareTo(BigDecimal.valueOf(10_000)) >= 0) throw new InvalidCapacityException("Capacidad inválida");
        if (price.compareTo(BigDecimal.ZERO) < 0 || price.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) throw new InvalidPriceException("Precio inválido");
        if (discount.compareTo(BigDecimal.ZERO) < 0 || discount.compareTo(new BigDecimal("100")) > 0) throw new ValidationException("Descuento inválido");
        return clubTablesDao.update(tableId, clubId, tableNumber.intValueExact(), capacity.intValueExact(), state, price, discount);
    }

    @Override
    @Transactional
    public boolean delete(UUID id) {
        return clubTablesDao.delete(id);
    }
}
