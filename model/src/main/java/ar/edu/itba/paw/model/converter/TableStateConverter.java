package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.TableState;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class TableStateConverter implements AttributeConverter<TableState, String> {
    @Override
    public String convertToDatabaseColumn(TableState tableState) {
        return tableState == null ? null : tableState.db();
    }
    @Override
    public TableState convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TableState.fromDb(dbData);
    }
}
