package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.dto.EventDto;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class EventDtoAssembler {

    public EventDto toDto(UriInfo uriInfo, UUID clubId, Event event) {
        EventDto dto = new EventDto();
        dto.setId(event.getEventId());
        dto.setEventName(event.getEventName());
        dto.setDescription(event.getDescription());
        dto.setDate(event.getDate());
        dto.setPhotoId(event.getPhotoId());

        String self = uriInfo.getBaseUriBuilder()
                .path(EventRestController.class)
                .path(EventRestController.class, "getById")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("eventId", event.getEventId())
                .build()
                .toString();

        String club = uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", clubId)
                .build()
                .toString();

        Map<String, String> links = new HashMap<>();
        links.put("self", self);
        links.put("club", club);

        if (event.getPhotoId() != null) {
            String photo = uriInfo.getBaseUriBuilder()
                    .path("clubs/{clubId}/events/{eventId}/photo")
                    .resolveTemplate("clubId", clubId)
                    .resolveTemplate("eventId", event.getEventId())
                    .build()
                    .toString();

            links.put("photo", photo);
        }

        dto.setLinks(links);
        return dto;
    }

    public EventDto toDto(UriInfo uriInfo, Event event) {
        return toDto(uriInfo, event.getClubId(), event);
    }
}