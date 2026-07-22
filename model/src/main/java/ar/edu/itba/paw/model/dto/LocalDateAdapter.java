package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;

public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return LocalDate.parse(value);
    }

    @Override
    public String marshal(LocalDate value) {
        return value == null ? null : value.toString();
    }
}
