package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.dto.EventDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public interface EventService {
    Event create(UUID clubId, String eventName, String description, LocalDate date);
    Event update(UUID clubId, UUID eventId, String eventName, String description, LocalDate date, Photo image);
    void delete(UUID clubId, UUID eventId);


    Event get(UUID eventId);

    Event getForClub(UUID clubId, UUID eventId);


    /** Lista eventos de un club con paginación (data) */
    List<Event> listByClub(UUID clubId, int page, int size);


    /** Total para paginación (count) */
    int countByClub(UUID clubId);




    /** Próximos eventos desde hoy (data) */
    List<Event> listUpcoming(int page, int size);

    /** Total de próximos eventos (count) */
    int countUpcoming();

    List<Event> findUpcomingByClub(UUID clubId, int offset, int limit);

    List<Event> findPastByClub(UUID clubId, int offset, int limit);

    int countUpcomingByClub(UUID clubId);

    int countPastByClub(UUID clubId);

    int totalEventPagesPast(UUID clubId,int size);

    int totalEventPagesUpcoming(UUID clubId ,int size);

    int totalEventPages(UUID clubId, int size);


    UUID setEventPhoto(UUID clubId, UUID eventId, Photo image);

    Photo getEventPhoto(UUID clubId, UUID eventId);

    void deleteEventPhoto(UUID clubId, UUID eventId);

    Optional<UUID> getEventPhotoId(UUID clubId, UUID eventId);

    Map<UUID, String> mapClubNamesForEvents(List<UUID> eventIds);
}
