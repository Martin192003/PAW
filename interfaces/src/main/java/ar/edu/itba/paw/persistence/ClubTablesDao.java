package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubTable;
import ar.edu.itba.paw.model.TableState;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClubTablesDao {
    Optional<ClubTable> findById(UUID id);
    List<ClubTable> findByClub(Club club);
    List<ClubTable> findAvailableByClubAndRange(UUID clubId, OffsetDateTime from, OffsetDateTime to);
    List<ClubTable> findAvailableByClubAndRangePaginated(final UUID clubId, final OffsetDateTime from, final OffsetDateTime to, final int limit, final int offset);
    int countAvailableByClubAndRange(final UUID clubId, final OffsetDateTime from, final OffsetDateTime to);
    ClubTable create(int number, int capacity, UUID clubId, TableState tableState, BigDecimal price, BigDecimal discount);
    ClubTable update(UUID tableId, UUID clubId, Integer tableNumber, Integer capacity, TableState state, BigDecimal price, BigDecimal discount);
    boolean delete(UUID id);
}
