package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.model.Status;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.UUID;

public class BookingSearchForm {

    @NotNull
    private UUID userId;

    private UUID clubId;

    private String bookingId;

    private Status status;

    @Pattern(regexp = "created_desc|created_asc|start_desc|start_asc|status_asc")
    private String sort = "created_desc";

    @Min(0)
    private int page = 0;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}