package ar.edu.itba.paw.model.dto;

import ar.edu.itba.paw.model.ProductType;
import java.math.BigDecimal;

public class ProductUpdateRequestDto {
    private String productName;
    private String description;
    private BigDecimal basePrice;
    private ProductType productType;
    private Boolean isAvailable;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public ProductType getProductType() { return productType; }
    public void setProductType(ProductType productType) { this.productType = productType; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}