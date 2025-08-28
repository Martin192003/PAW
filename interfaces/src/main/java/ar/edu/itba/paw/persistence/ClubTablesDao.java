// ClubTablesDao.java
package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.Club;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ClubTablesDao {
    Table findById(UUID id);
    List<Table> findAll();
    List<Table> findByClub(Club club);

    Table create(int number, int capacity, UUID clubId, String tableState, BigDecimal price, BigDecimal discount);
    Table update(Table table);
    boolean delete(UUID id);
}
