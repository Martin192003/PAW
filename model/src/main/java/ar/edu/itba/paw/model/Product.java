package ar.edu.itba.paw.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class Product {

    private UUID productId;
    private Club club;
    private String productName;
    private String description;
    private BigDecimal basePrice;
    private ProductType productType;
    private Boolean isAvailable = Boolean.TRUE;
    private OffsetDateTime createdAt;

    public Product() {}

    public Product(UUID productId, Club club, String productName, String description,
                   BigDecimal basePrice, ProductType productType, Boolean isAvailable,
                   OffsetDateTime createdAt) {
        this.productId = productId;
        this.club = club;
        this.productName = productName;
        this.description = description;
        this.basePrice = basePrice;
        this.productType = productType;
        this.isAvailable = isAvailable;
        this.createdAt = createdAt;
    }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Club getClub() { return club; }
    public void setClub(Club club) { this.club = club; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean available) { isAvailable = available; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
