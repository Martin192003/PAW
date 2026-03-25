package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.UUID;

@Component
@Path("/clubs/events/{clubId}/{eventId}/photo")
public class EventPhotoRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private EventDtoAssembler eventDtoAssembler;

    @Context
    private HttpServletRequest request;

    @Context
    private UriInfo uriInfo;

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putPhoto(@PathParam("clubId") UUID clubId,
                             @PathParam("eventId") UUID eventId)
            throws IOException, ServletException {

        clubService.getClubById(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found"));

        Part filePart = request.getPart("file");
        if (filePart == null) {
            throw new BadRequestException("Missing multipart part 'file'");
        }
        if (filePart.getSize() == 0) {
            throw new BadRequestException("Empty file");
        }

        String contentType = filePart.getContentType();
        validateImageContentType(contentType);

        byte[] data = filePart.getInputStream().readAllBytes();

        Photo photo = new Photo();
        photo.setFilename(extractFilename(filePart));
        photo.setContentType(contentType);
        photo.setData(data);
        photo.setSizeBytes((long) data.length);

        eventService.setEventPhoto(clubId, eventId, photo);

        Event updatedEvent = eventService.get(eventId);
        EventDto dto = eventDtoAssembler.toDto(uriInfo, clubId, updatedEvent);

        return Response.ok(dto).build();
    }

    @GET
    public Response getPhoto(@PathParam("clubId") UUID clubId,
                             @PathParam("eventId") UUID eventId) {

        Photo photo = eventService.getEventPhoto(clubId, eventId);

        return Response.ok(photo.getData())
                .type(photo.getContentType())
                .header("Content-Disposition",
                        "inline; filename=\"" + safeFilename(photo.getFilename()) + "\"")
                .build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePhoto(@PathParam("clubId") UUID clubId,
                                @PathParam("eventId") UUID eventId) {

        eventService.deleteEventPhoto(clubId, eventId);

        Event updatedEvent = eventService.get(eventId);
        EventDto dto = eventDtoAssembler.toDto(uriInfo, clubId, updatedEvent);

        return Response.ok(dto).build();
    }

    private void validateImageContentType(String contentType) {
        if (contentType == null) {
            throw new BadRequestException("Missing content type");
        }

        switch (contentType) {
            case "image/jpeg":
            case "image/png":
            case "image/webp":
                return;
            default:
                throw new BadRequestException("Unsupported image content type");
        }
    }

    private String extractFilename(Part part) {
        String filename = part.getSubmittedFileName();
        return (filename == null || filename.trim().isEmpty()) ? "event-photo" : filename;
    }

    private String safeFilename(String filename) {
        return (filename == null || filename.trim().isEmpty())
                ? "event-photo"
                : filename.replace("\"", "");
    }
}