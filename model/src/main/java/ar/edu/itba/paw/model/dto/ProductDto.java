package ar.edu.itba.paw.model.dto;

import ar.edu.itba.paw.model.ProductType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ProductDto {

    private UUID productId;
    private String productName;
    private String description;
    private BigDecimal basePrice;
    private ProductType productType;
    private Boolean isAvailable;
    private OffsetDateTime createdAt;
    private List<LinkDto> links;

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    public List<LinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
