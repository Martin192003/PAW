package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
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

    // Mismo bug que EventDto.date: sin el adapter, MOXy usa el patron de
    // datetime global (JaxRsConfig) y rechaza el "yyyy-MM-dd" simple que
    // manda el frontend al reservar una mesa.
    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
