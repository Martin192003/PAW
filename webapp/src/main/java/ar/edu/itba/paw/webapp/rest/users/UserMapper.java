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
        return dto;
    }

    public static UserDto toDto(User user, UriInfo uriInfo) {
        UserDto dto = toDto(user);
        if (dto == null || uriInfo == null) {
            return dto;
        }

        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("users/{userId}")
                .resolveTemplate("userId", dto.getUserId())
                .build(), "GET"));
        links.add(LinkBuilder.link("bookings", uriInfo.getBaseUriBuilder()
                .path("bookings")
                .build(), "GET"));
        links.add(LinkBuilder.link("reviews", uriInfo.getBaseUriBuilder()
                .path("users/{userId}/reviews")
                .resolveTemplate("userId", dto.getUserId())
                .build(), "GET"));
        links.add(LinkBuilder.link("clubs", uriInfo.getBaseUriBuilder()
                .path("clubs/user/{userId}")
                .resolveTemplate("userId", dto.getUserId())
                .build(), "GET"));
        dto.setLinks(links);

        return dto;
    }
}
