package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Component
@Path("/photos")
public class PhotoRestController {

    private final PhotoService photoService;

    public PhotoRestController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GET
    @Path("/{photoId}")
    public Response getPhoto(@PathParam("photoId") String photoId) {
        UUID id;
        try {
            id = UUID.fromString(photoId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("photoId inválido"))
                    .build();
        }

        return photoService.getWithId(id)
                .map(p -> Response.ok(p.getData(), p.getContentType())
                        .header("Content-Length", p.getSizeBytes())
                        .build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorDto("Foto no encontrada"))
                        .build());
    }
}
