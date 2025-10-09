package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.Status;

import javax.persistence.AttributeConverter;

public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status status) {
        return status == null ? null : status.db();

    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Status.fromDb(dbData);
    }
}
