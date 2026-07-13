package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;

/**
 * JaxRsConfig registra globalmente "eclipselink.json.date-format" con un patron
 * de datetime completo (yyyy-MM-dd'T'HH:mm:ss.SSSXXX) para que las fechas con
 * hora (ej. createdAt) se serialicen bien. Ese mismo patron global lo usa MOXy
 * para CUALQUIER campo de tipo fecha, incluidos los LocalDate simples como
 * "date" en EventDto/BookingCreateRequestDto.
 *
 * El problema: el frontend manda <input type="date">, que produce un string
 * "yyyy-MM-dd" (ej. "2026-07-22"), sin la parte de hora. MOXy intenta
 * parsearlo con el patron global (que exige 'T', hora y offset) y falla al
 * deserializar el body — antes de que el controller o el service lleguen a
 * ejecutarse. Como no hay ExceptionMapper para ese fallo de unmarshalling,
 * el request termina en un 500 crudo sin ningun mensaje util.
 *
 * Este adapter fuerza a MOXy a tratar el campo como un String plano
 * "yyyy-MM-dd" en JSON, ignorando el patron global para ese campo puntual.
 */
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
