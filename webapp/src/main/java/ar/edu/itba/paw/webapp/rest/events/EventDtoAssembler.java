package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
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

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", self, "GET"));
        links.add(LinkBuilder.link("club", club, "GET"));

        String photo = uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/events/{eventId}/photo")
                .resolveTemplate("clubId", clubId)
                .resolveTemplate("eventId", event.getEventId())
                .build()
                .toString();
        links.add(LinkBuilder.link("photo", photo, "GET"));
        links.add(LinkBuilder.link("update", self, "PUT"));
        links.add(LinkBuilder.link("delete", self, "DELETE"));

        dto.setLinks(links);
        return dto;
    }

    public EventDto toDto(UriInfo uriInfo, Event event) {
        return toDto(uriInfo, event.getClubId(), event);
    }

    public EventDto toDto(UriInfo uriInfo, UUID clubId, Event event, String clubName) {
        EventDto dto = toDto(uriInfo, clubId, event);
        dto.setClubName(clubName);
        return dto;
    }

    public EventDto toDto(UriInfo uriInfo, Event event, String clubName) {
        return toDto(uriInfo, event.getClubId(), event, clubName);
    }
}
