package ar.edu.itba.paw.model.dto;

import java.util.List;

public class PagedResponseDto {

    private List<ClubDto> content;
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;

    private boolean availableToday;
    private boolean invalidDay;
    private String selectedAvailabilityDate;

    public PagedResponseDto() {
    }

    public PagedResponseDto(List<ClubDto> content,
                            int page,
                            int size,
                            int totalElements,
                            int totalPages,
                            boolean availableToday,
                            boolean invalidDay,
                            String selectedAvailabilityDate) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.availableToday = availableToday;
        this.invalidDay = invalidDay;
        this.selectedAvailabilityDate = selectedAvailabilityDate;
    }

    public List<ClubDto> getContent() {
        return content;
    }

    public void setContent(List<ClubDto> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(int totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isAvailableToday() {
        return availableToday;
    }

    public void setAvailableToday(boolean availableToday) {
        this.availableToday = availableToday;
    }

    public boolean isInvalidDay() {
        return invalidDay;
    }

    public void setInvalidDay(boolean invalidDay) {
        this.invalidDay = invalidDay;
    }

    public String getSelectedAvailabilityDate() {
        return selectedAvailabilityDate;
    }

    public void setSelectedAvailabilityDate(String selectedAvailabilityDate) {
        this.selectedAvailabilityDate = selectedAvailabilityDate;
    }
}
