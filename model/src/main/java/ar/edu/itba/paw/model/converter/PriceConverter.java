package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.Price;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class PriceConverter implements AttributeConverter<Price, String> {

    @Override
    public String convertToDatabaseColumn(Price attr) {
        return attr == null ? null : attr.name().toLowerCase(); // store lowercase
    }

    @Override
    public Price convertToEntityAttribute(String db) {
        if (db == null) return null;
        switch (db.trim().toLowerCase()) {
            case "bajo": case "low": case "$":    return Price.BAJO;
            case "medio": case "medium": case "$$": return Price.MEDIO;
            case "alto": case "high": case "$$$": return Price.ALTO;
            default:
                throw new IllegalArgumentException("Unknown price_range: " + db);
        }
    }
}