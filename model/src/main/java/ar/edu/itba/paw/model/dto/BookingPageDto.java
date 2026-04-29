package ar.edu.itba.paw.model.dto;

import java.util.List;

public class BookingPageDto {
    private List<BookingDto> content;
    private int page;
    private int size;
    private int totalElements;
    private int totalPages;

    public BookingPageDto() {
    }

    public BookingPageDto(List<BookingDto> content, int page, int size, int totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<BookingDto> getContent() {
        return content;
    }

    public void setContent(List<BookingDto> content) {
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
}
