package ar.edu.itba.paw.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class BookingDto {
    private UUID id;
    private UUID clubId;
    private String clubName;
    private UUID userId;
    private String userUsername;
    private String userName;
    private String userSurname;
    private UUID tableId;
    private Integer tableNumber;
    private BigDecimal tablePrice;
    private BigDecimal tableDiscount;
    private LocalDate date;
    private OffsetDateTime bookingStart;
    private OffsetDateTime bookingEnd;
    private String status;
    private OffsetDateTime createdAt;
    private BigDecimal totalAmount;
    private boolean hasReceipt;
    private List<LinkDto> links;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getClubId() {
        return clubId;
    }

    public void setClubId(UUID clubId) {
        this.clubId = clubId;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserSurname() {
        return userSurname;
    }

    public void setUserSurname(String userSurname) {
        this.userSurname = userSurname;
    }

    public UUID getTableId() {
        return tableId;
    }

    public void setTableId(UUID tableId) {
        this.tableId = tableId;
    }

    public Integer getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(Integer tableNumber) {
        this.tableNumber = tableNumber;
    }

    public BigDecimal getTablePrice() {
        return tablePrice;
    }

    public void setTablePrice(BigDecimal tablePrice) {
        this.tablePrice = tablePrice;
    }

    public BigDecimal getTableDiscount() {
        return tableDiscount;
    }

    public void setTableDiscount(BigDecimal tableDiscount) {
        this.tableDiscount = tableDiscount;
    }

    @XmlJavaTypeAdapter(LocalDateAdapter.class)
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public OffsetDateTime getBookingStart() {
        return bookingStart;
    }

    public void setBookingStart(OffsetDateTime bookingStart) {
        this.bookingStart = bookingStart;
    }

    public OffsetDateTime getBookingEnd() {
        return bookingEnd;
    }

    public void setBookingEnd(OffsetDateTime bookingEnd) {
        this.bookingEnd = bookingEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean isHasReceipt() {
        return hasReceipt;
    }

    public void setHasReceipt(boolean hasReceipt) {
        this.hasReceipt = hasReceipt;
    }

    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
