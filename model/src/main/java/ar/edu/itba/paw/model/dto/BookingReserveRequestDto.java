package ar.edu.itba.paw.model.dto;

public class BookingReserveRequestDto {
    private String tableId;
    private String date;
    private Boolean fromEvent;

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Boolean getFromEvent() {
        return fromEvent;
    }

    public void setFromEvent(Boolean fromEvent) {
        this.fromEvent = fromEvent;
    }
}

