package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.converter.TableStateConverter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "club_tables")
public class ClubTable {

    @Id
    @Column(name = "table_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID tableid;

    @Column(name = "club_id", columnDefinition = "UUID", nullable = false)
    private UUID clubId;

    @Column(name = "table_number", nullable = false)
    private int tableNumber;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Convert(converter = TableStateConverter.class)
    @Column(name = "table_state", nullable = false)
    private TableState tableState; // disponible, pendiente, reservada

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "discount", precision = 5, scale = 2, nullable = false)
    private BigDecimal discount;   // porcentaje 0..100

    public ClubTable() {}

    public ClubTable(UUID id, UUID clubID, int tableNumber, int capacity, TableState state,
                 BigDecimal price, BigDecimal discount) {
        this.tableid = id;
        this.clubId = clubID;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.tableState = state == null ? TableState.DISPONIBLE : state;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.discount = discount == null ? BigDecimal.ZERO : discount;
    }

    @PrePersist
    private void prePersist() {
        if (tableid == null) tableid = UUID.randomUUID();
        if (tableState == null) tableState = TableState.DISPONIBLE;
        if (price == null) price = BigDecimal.ZERO;
        if (discount == null) discount = BigDecimal.ZERO;
    }

    public UUID getTableId() { return tableid; }
    public void setTableId(UUID tableId) { this.tableid = tableId; }

    public UUID getClubId() { return clubId; }
    public void setClubId(UUID club) { this.clubId = club; }

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public TableState getTableState() { return tableState; }
    public void setTableState(TableState tableState) { this.tableState = tableState; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
}
