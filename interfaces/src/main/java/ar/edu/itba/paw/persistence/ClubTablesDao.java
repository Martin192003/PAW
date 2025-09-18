// ClubTablesDao.java
package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.TableState;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubTablesDao {
    Optional<Table> findById(UUID id);
    List<Table> findAll();
    List<Table> findByClub(Club club);
    List<Table> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to);

    Table create(int number, int capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount);
    Table update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity, TableState state, BigDecimal price, BigDecimal discount);
    boolean delete(UUID id);
}
