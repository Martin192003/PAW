package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableService {
    Optional<Table> findById(UUID id);
    List<Table> findByClubId(UUID clubId);
    List<Table> findAll();
    List<Table> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to);

    Table create(BigDecimal number, BigDecimal capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount);
    Table update(UUID tableId, UUID clubId, BigDecimal tableNumber, BigDecimal capacity, TableState state, BigDecimal price, BigDecimal discount);
    boolean delete(UUID id);

}
