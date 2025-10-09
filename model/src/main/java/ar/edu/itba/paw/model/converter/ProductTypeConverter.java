package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.ProductType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class ProductTypeConverter implements AttributeConverter<ProductType, String> {
    @Override
    public String convertToDatabaseColumn(ProductType productType) {
        return productType == null ? null : productType.db();
    }
    @Override
    public ProductType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ProductType.fromDb(dbData);
    }
}
