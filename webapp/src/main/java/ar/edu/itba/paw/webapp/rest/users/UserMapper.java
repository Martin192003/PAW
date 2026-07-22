package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.LinkDto;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public final class UserMapper {
    private UserMapper() {}


    public static UserDto toDto(User user) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setCellphone(user.getCellphone());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLocale(user.getLocale());
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setVerified(user.isVerified());
        return dto;
    }

    public static UserDto toDto(User user, UriInfo uriInfo) {
        UserDto dto = toDto(user);
        if (dto == null || uriInfo == null) {
            return dto;
        }
        dto.setLinks(buildPrivateLinks(dto.getUserId().toString(), uriInfo));
        return dto;
    }


    public static UserDto toPublicDto(User user, UriInfo uriInfo) {
        if (user == null) return null;
        UserDto dto = new UserDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());

        if (uriInfo != null) {
            String uid = user.getId().toString();
            List<LinkDto> links = new ArrayList<>();
            links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                    .path("users/{id}").resolveTemplate("id", uid).build(), "GET"));
            links.add(LinkBuilder.link("reviews", uriInfo.getBaseUriBuilder()
                    .path("users/{id}/reviews").resolveTemplate("id", uid).build(), "GET"));
            dto.setLinks(links);
        }
        return dto;
    }


    private static List<LinkDto> buildPrivateLinks(String userId, UriInfo uriInfo) {
        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("users/{id}").resolveTemplate("id", userId).build(), "GET"));
        links.add(LinkBuilder.link("bookings", uriInfo.getBaseUriBuilder()
                .path("users/{id}/bookings").resolveTemplate("id", userId).build(), "GET"));
        links.add(LinkBuilder.link("managed", uriInfo.getBaseUriBuilder()
                .path("bookings").queryParam("managed", "true").build(), "GET"));
        links.add(LinkBuilder.link("reviews", uriInfo.getBaseUriBuilder()
                .path("users/{id}/reviews").resolveTemplate("id", userId).build(), "GET"));
        links.add(LinkBuilder.link("clubs", uriInfo.getBaseUriBuilder()
                .path("clubs").queryParam("ownerId", userId).build(), "GET"));
        return links;
    }
}
