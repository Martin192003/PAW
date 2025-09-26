package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.model.ProductType;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public class AddProductForm {
    private UUID clubId;

    @NotBlank(message = "{product.name.notblank}")
    @Size(max = 100, message = "{error.name}")
    private String productName;

    @NotBlank(message = "{product.description.notblank}")
    private String description;

    @NotNull(message = "{product.price.notnull}")
    @DecimalMax(value = "100000000", message = "{product.price.max}")
    @DecimalMin(value = "0", message = "{product.price.min}")
    private BigDecimal basePrice;


    @NotNull(message = "{product.type.notnull}")
    private ProductType productType;

    private Boolean isAvailable = Boolean.TRUE;

    // Getters y Setters
    public UUID getClubId() { return clubId; }
    public void setClubId(UUID clubId) { this.clubId = clubId; }

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

