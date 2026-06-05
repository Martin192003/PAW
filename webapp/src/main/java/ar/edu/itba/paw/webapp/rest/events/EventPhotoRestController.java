package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Event;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.webapp.rest.ContentDispositionHeader;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.UUID;

@Component
@Path("/clubs/{clubId}/events/{eventId}/photo")
public class EventPhotoRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private EventDtoAssembler eventDtoAssembler;

    @Autowired
    private AuthenticatedUserResolver authenticatedUserResolver;

    @Context
    private HttpServletRequest request;

    @Context
    private UriInfo uriInfo;

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putPhoto(@PathParam("clubId") UUID clubId,
                             @PathParam("eventId") UUID eventId,
                             @Context SecurityContext securityContext)
            throws IOException, ServletException {

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubId, user.getId());
        eventService.getForClub(clubId, eventId);

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

        Event updatedEvent = eventService.getForClub(clubId, eventId);
        EventDto dto = eventDtoAssembler.toDto(uriInfo, clubId, updatedEvent);

        return Response.ok(dto).build();
    }

    @GET
    public Response getPhoto(@PathParam("clubId") UUID clubId,
                             @PathParam("eventId") UUID eventId) {

        Photo photo = eventService.getEventPhoto(clubId, eventId);

        return Response.ok(photo.getData())
                .type(photo.getContentType())
                .header("Content-Disposition", ContentDispositionHeader.inline(photo.getFilename(), "event-photo"))
                .build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePhoto(@PathParam("clubId") UUID clubId,
                                @PathParam("eventId") UUID eventId,
                                @Context SecurityContext securityContext) {

        User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(clubId, user.getId());
        eventService.getForClub(clubId, eventId);
        eventService.deleteEventPhoto(clubId, eventId);

        Event updatedEvent = eventService.getForClub(clubId, eventId);
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

}
