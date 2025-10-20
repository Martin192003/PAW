package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface EventService {
    Event create(UUID clubId, String eventName, String description, LocalDate date, UUID photoId);
    Event update(UUID clubId, UUID eventId, String eventName, String description, LocalDate date, UUID photoId);
    void delete(UUID clubId, UUID eventId);


    Event get(UUID eventId);


    /** Lista eventos de un club con paginación (data) */
    List<Event> listByClub(UUID clubId, int page, int size);


    /** Total para paginación (count) */
    int countByClub(UUID clubId);


    /** Búsqueda por rango de fechas con paginación (data) */
    List<Event> searchByDate(LocalDate start, LocalDate end, int page, int size);


    /** Total para paginación (count) */
    int countByDate(LocalDate start, LocalDate end);
}