package ar.edu.itba.paw.model;

import java.math.BigDecimal;
import java.util.UUID;


public class BookingItem {

    private UUID bookingItemId;
    private Booking booking;
    private Product product;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public BookingItem() {}

    public BookingItem(UUID bookingItemId, Booking booking, Product product,
                       Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        this.bookingItemId = bookingItemId;
        this.booking = booking;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
    }

    public UUID getBookingItemId() { return bookingItemId; }
    public void setBookingItemId(UUID bookingItemId) { this.bookingItemId = bookingItemId; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
