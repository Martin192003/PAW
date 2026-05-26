package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.PhotoService;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.*;
import java.util.Objects;
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
    public Response getPhoto(@PathParam("photoId") String photoId, @Context Request request) {
        UUID id;
        try {
            id = UUID.fromString(photoId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("photoId inválido"))
                    .build();
        }

        return photoService.getWithId(id)
                .map(photo -> buildImageResponse(photo, request))
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorDto("Foto no encontrada"))
                        .build());
    }

    /**
     * Construye la respuesta de imagen con caching condicional correcto.
     *
     * Las fotos son recursos modificables (se pueden reemplazar o eliminar), por lo que NO
     * se debe usar cache largo (max-age anual). Se usa:
     *   Cache-Control: no-cache
     *   ETag: basado en photoId + createdAt (estable, cambia al re-subir con distinto ID)
     *
     * El cliente puede cachear localmente pero DEBE revalidar con el servidor antes de usar
     * la copia cacheada. Si el ETag coincide, el servidor responde 304 Not Modified (sin body).
     */
    private Response buildImageResponse(Photo photo, Request request) {
        // ETag basado en photoId + createdAt: es estable mientras la foto no cambie,
        // y varía si la foto es reemplazada (nuevo UUID o nueva fecha de creación).
        String etagValue = photo.getPhotoId().toString()
                + (photo.getCreatedAt() != null ? photo.getCreatedAt().toEpochSecond() : "0");
        EntityTag etag = new EntityTag(Integer.toHexString(etagValue.hashCode()));

        Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

        // no-cache: el cliente debe revalidar antes de usar la copia cacheada
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setPrivate(false);

        if (preconditions != null) {
            // 304 Not Modified
            return preconditions
                    .cacheControl(cacheControl)
                    .tag(etag)
                    .build();
        }

        return Response.ok(photo.getData(), photo.getContentType())
                .cacheControl(cacheControl)
                .tag(etag)
                .header("Content-Length", photo.getSizeBytes())
                .build();
    }
}
