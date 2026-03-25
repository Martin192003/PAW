package ar.edu.itba.paw.webapp.rest.events;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.exceptions.NotFoundException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.EventService;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

@Component
@Path("/clubs/{clubId}/events/{eventId}/photo")
public class EventPhotoRestController {

    @Autowired
    private EventService eventService;

    @Autowired
    private PhotoService photoService;

    @Autowired
    private ClubService clubService;

    @Context
    private HttpServletRequest request;

    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
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

        return Response.noContent().build();
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
    public Response deletePhoto(@PathParam("clubId") UUID clubId,
                                @PathParam("eventId") UUID eventId) {

        eventService.deleteEventPhoto(clubId, eventId);
        return Response.noContent().build();
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