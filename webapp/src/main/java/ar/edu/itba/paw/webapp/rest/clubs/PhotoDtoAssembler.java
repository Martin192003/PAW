package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.PhotoDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

@Component
public class PhotoDtoAssembler {

    public PhotoDto toDto(UriInfo uriInfo, Photo photo) {
        PhotoDto dto = new PhotoDto();
        dto.setPhotoId(photo.getPhotoId());
        dto.setClubId(photo.getClubId());
        dto.setPostedBy(photo.getPostedBy());
        dto.setReviewId(photo.getReviewId());
        dto.setContentType(photo.getContentType());
        dto.setSizeBytes(photo.getSizeBytes());
        dto.setPhotoType(photo.getPhotoType() != null ? photo.getPhotoType().name() : null);
        dto.setCreatedAt(photo.getCreatedAt());

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("photos/{photoId}")
                .resolveTemplate("photoId", photo.getPhotoId())
                .build(), "GET"));
        links.add(LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}")
                .resolveTemplate("clubId", photo.getClubId())
                .build(), "GET"));
        // El DELETE real vive en ClubRestController (/clubs/{clubId}/photos/{photoId},
        // sólo admin del club), no en este mismo controller. Lo exponemos igual como link
        // para que el frontend no tenga que armar la URL a mano concatenando ids; el server
        // sigue siendo quien valida el permiso al momento de invocarlo.
        links.add(LinkBuilder.link("delete", uriInfo.getBaseUriBuilder()
                .path("clubs/{clubId}/photos/{photoId}")
                .resolveTemplate("clubId", photo.getClubId())
                .resolveTemplate("photoId", photo.getPhotoId())
                .build(), "DELETE"));
        if (photo.getReviewId() != null) {
            links.add(LinkBuilder.link("review", uriInfo.getBaseUriBuilder()
                    .path("clubs/{clubId}/reviews/{reviewId}")
                    .resolveTemplate("clubId", photo.getClubId())
                    .resolveTemplate("reviewId", photo.getReviewId())
                    .build(), "GET"));
        }
        dto.setLinks(links);

        return dto;
    }
}
