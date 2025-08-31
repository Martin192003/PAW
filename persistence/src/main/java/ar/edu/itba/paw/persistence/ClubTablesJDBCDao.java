package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Table;
import ar.edu.itba.paw.model.TableState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class ClubTablesJDBCDao implements ClubTablesDao {

    private final JdbcTemplate jdbc;
    private final SimpleJdbcInsert insert;

    private static final RowMapper<Table> ROW_MAPPER = (rs, i) -> {
        Table t = new Table();
        t.setTableId(rs.getObject("table_id", java.util.UUID.class));
        Club c = new Club(); c.setClubId(rs.getObject("club_id", java.util.UUID.class)); t.setClub(c);
        t.setTableNumber(rs.getInt("table_number"));
        t.setCapacity(rs.getInt("capacity"));
        t.setTableState(TableState.valueOf(rs.getString("table_state").toUpperCase())); // 'disponible','pendiente','reservada'
        t.setPrice(rs.getBigDecimal("price"));
        t.setDiscount(rs.getBigDecimal("discount"));
        return t;
    };

    @Autowired
    public ClubTablesJDBCDao(final DataSource ds) {
        this.jdbc = new JdbcTemplate(ds);
        this.insert = new SimpleJdbcInsert(jdbc)
                .withTableName("club_tables")
                .usingColumns("table_id","club_id","table_number","capacity","table_state","price","discount");
    }

    @Override
    public Table create(int number, int capacity, java.util.UUID clubId, String tableState, BigDecimal price, BigDecimal discount) {
        java.util.UUID id = java.util.UUID.randomUUID();
        Map<String,Object> params = new HashMap<>();
        params.put("table_id", id);
        params.put("club_id", clubId);
        params.put("table_number", number);
        params.put("capacity", capacity);
        params.put("table_state", tableState);
        params.put("price", price);
        params.put("discount", discount);
        insert.execute(params);
        return findById(id);
    }

    @Override
    public Table update(Table table) {
        final String sql = "UPDATE club_tables SET club_id=?, table_number=?, capacity=?, table_state=?, price=?, discount=? WHERE table_id=?";
        int upd = jdbc.update(sql,
                table.getClub().getClubId(),
                table.getTableNumber(),
                table.getCapacity(),
                table.getTableState(),
                table.getPrice(),
                table.getDiscount(),
                table.getTableId());
        return upd > 0 ? findById(table.getTableId()) : null;
    }

    @Override
    public boolean delete(java.util.UUID id) {
        return jdbc.update("DELETE FROM club_tables WHERE table_id = ?", id) > 0;
    }

    @Override
    public Table findById(java.util.UUID id) {
        final String sql = "SELECT table_id,club_id,table_number,capacity,table_state,price,discount FROM club_tables WHERE table_id = ?";
        return jdbc.query(sql, ROW_MAPPER, id).stream().findFirst().orElse(null);
    }

    @Override
    public List<Table> findAll() {
        final String sql = "SELECT table_id,club_id,table_number,capacity,table_state,price,discount FROM club_tables ORDER BY table_number ASC";
        return jdbc.query(sql, ROW_MAPPER);
    }

    @Override
    public List<Table> findByClub(Club club) {
        final String sql = "SELECT table_id,club_id,table_number,capacity,table_state,price,discount FROM club_tables WHERE club_id = ? ORDER BY table_number ASC";
        return jdbc.query(sql, ROW_MAPPER, club.getClubId());
    }

}
