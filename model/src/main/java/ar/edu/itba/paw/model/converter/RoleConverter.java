package ar.edu.itba.paw.model.converter;

import ar.edu.itba.paw.model.Role;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {
    @Override
    public String convertToDatabaseColumn(Role attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }
    @Override
    public Role convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Role.valueOf(dbData.toUpperCase());
    }
}