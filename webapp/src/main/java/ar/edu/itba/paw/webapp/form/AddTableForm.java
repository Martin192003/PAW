package ar.edu.itba.paw.webapp.form;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.UUID;

public class AddTableForm {
    private UUID clubId;

    @NotNull(message = "{table.number.notnull}")
    @Min(value = 1, message = "{table.number.min}")
    private BigDecimal tableNumber;

    @NotNull(message = "{table.capacity.notnull}")
    @Min(value = 1, message = "{table.capacity.min}")
    private BigDecimal capacity;

    @NotNull(message = "{table.price.notnull}")
    @DecimalMax(value = "100000000", message = "{table.price.max}")
    @DecimalMin(value = "0", message = "{table.price.min}")
    private BigDecimal price;

    @DecimalMax(value = "100", message = "{table.discount}")
    @DecimalMin(value = "0", message = "{table.discount}")
    private BigDecimal discount = BigDecimal.ZERO;

    // Getters y Setters
    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

    public BigDecimal getTableNumber() { return tableNumber; }
    public void setTableNumber(BigDecimal tableNumber) { this.tableNumber = tableNumber; }

    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
}
