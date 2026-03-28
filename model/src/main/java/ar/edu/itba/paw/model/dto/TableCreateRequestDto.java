package ar.edu.itba.paw.model.dto;

import ar.edu.itba.paw.model.TableState;

import java.math.BigDecimal;

public class TableCreateRequestDto {
    private Integer tableNumber;
    private Integer capacity;
    private BigDecimal price;
    private BigDecimal discount;
    private TableState tableState;

    public TableCreateRequestDto() {}

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public TableState getTableState() {
        return tableState;
    }

    public void setTableState(TableState tableState) {
        this.tableState = tableState;
    }
}