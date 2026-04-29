package ar.edu.itba.paw.model.dto;

import java.time.LocalDate;
import java.util.UUID;

public class BookingCreateRequestDto {
    private UUID tableId;
    private LocalDate date;

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
