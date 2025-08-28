package ar.edu.itba.paw.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class Table {

    private UUID tableId;
    private Club club;
    private int tableNumber;
    private int capacity;
    private TableState tableState; //  disponible, pendiente, reservada
    private BigDecimal price;
    private BigDecimal discount;   // porcentaje 0..100

    public Table() {}

    public Table(Club club, int tableNumber, int capacity, TableState state, BigDecimal price, BigDecimal discount) {
        this.tableId = UUID.randomUUID();
        this.club = club;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.tableState = state == null ? TableState.DISPONIBLE : state;
        this.price = price == null ? BigDecimal.ZERO : price;
        this.discount = discount == null ? BigDecimal.ZERO : discount;
    }

    public UUID getTableId() { return tableId; }
    public void setTableId(UUID tableId) { this.tableId = tableId; }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }

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
