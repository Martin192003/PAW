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

    private CacheControl buildCacheControl() {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(31536000);
        cc.setPrivate(false);
        cc.setNoTransform(false);
        return cc;
    }

    private Response buildImageResponse(Photo photo, Request request) {

        EntityTag etag = new EntityTag(
                Integer.toHexString(
                        Objects.hash(photo.getPhotoId(), photo.getSizeBytes())
                )
        );

        Response.ResponseBuilder preconditions =
                request.evaluatePreconditions(etag);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(31536000); // 1 año
        cacheControl.setPrivate(false);

        if (preconditions != null) {
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
