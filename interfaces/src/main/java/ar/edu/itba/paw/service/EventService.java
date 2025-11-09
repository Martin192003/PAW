package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public interface EventService {
    Event create(UUID clubId, String eventName, String description, LocalDate date, Photo image);
    Event update(UUID clubId, UUID eventId, String eventName, String description, LocalDate date, Photo image);
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


    /** Próximos eventos desde hoy (data) */
    List<Event> listUpcoming(int page, int size);

    /** Total de próximos eventos (count) */
    int countUpcoming();

    public boolean isEventPhoto(UUID clubId, UUID photoId);

    Set<UUID> getEventPhotoIds(UUID clubId);

    List<Event> findUpcomingByClub(UUID clubId, int offset, int limit);

    List<Event> findPastByClub(UUID clubId, int offset, int limit);

    int countUpcomingByClub(UUID clubId);

    int countPastByClub(UUID clubId);


}