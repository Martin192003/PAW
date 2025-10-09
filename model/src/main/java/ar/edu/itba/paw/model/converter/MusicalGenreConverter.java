package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.MusicalGenre;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = false)
public class MusicalGenreConverter implements AttributeConverter<MusicalGenre, String> {
    @Override
    public String convertToDatabaseColumn(MusicalGenre attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public MusicalGenre convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return MusicalGenre.valueOf(dbData.trim().toUpperCase());
    }
}