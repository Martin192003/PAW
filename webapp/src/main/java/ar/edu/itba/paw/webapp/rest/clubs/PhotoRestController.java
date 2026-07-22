package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.PhotoType;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.ErrorDto;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.webapp.rest.auth.AuthenticatedUserResolver;
import org.springframework.stereotype.Component;

import javax.ws.rs.DELETE;
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
    private final ClubService clubService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public PhotoRestController(PhotoService photoService,
                               ClubService clubService,
                               AuthenticatedUserResolver authenticatedUserResolver) {
        this.photoService = photoService;
        this.clubService = clubService;
        this.authenticatedUserResolver = authenticatedUserResolver;
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

    @DELETE
    @Path("/{photoId}")
    public Response deletePhoto(@PathParam("photoId") String photoId,
                                @Context SecurityContext securityContext) {
        final UUID id;
        try {
            id = UUID.fromString(photoId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("photoId inválido"))
                    .build();
        }

        final Photo photo = photoService.getWithId(id)
                .orElse(null);
        if (photo == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("Foto no encontrada"))
                    .build();
        }

        final User user = authenticatedUserResolver.resolve(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(photo.getClubId(), user.getId());


        if (photo.getPhotoType() != null && photo.getPhotoType() != PhotoType.CAROUSEL) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorDto("Solo se pueden eliminar fotos de galería"))
                    .build();
        }
        if (!photoService.delete(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorDto("Foto no encontrada"))
                    .build();
        }
        return Response.noContent().build();
    }


    private Response buildImageResponse(Photo photo, Request request) {

        String etagValue = photo.getPhotoId().toString()
                + (photo.getCreatedAt() != null ? photo.getCreatedAt().toEpochSecond() : "0")
                + Objects.toString(photo.getSizeBytes(), "0")
                + Objects.toString(photo.getContentType(), "");
        EntityTag etag = new EntityTag(Integer.toHexString(etagValue.hashCode()));

        Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

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
