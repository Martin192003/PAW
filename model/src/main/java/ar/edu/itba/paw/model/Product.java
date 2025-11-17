package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.converter.ProductTypeConverter;

import javax.persistence.*;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "product_id", columnDefinition = "UUID", nullable = false, updatable = false)
    private UUID productId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "product_name", nullable = false, unique = true)
    private String productName;

    @Column(name = "description")
    private String description;

    @Column(name = "base_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @Convert(converter = ProductTypeConverter.class)
    @Column(name = "product_type", nullable = false)
    private ProductType productType;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = Boolean.TRUE;

    @Column(name = "created_at")
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

    @PrePersist
    private void prePersist() {
        if (productId == null) productId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (isAvailable == null) isAvailable = Boolean.TRUE;
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
