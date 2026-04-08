package ar.edu.itba.paw.webapp.rest.users;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.dto.UserDto;

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
        dto.setRole(user.getRole() != null ? user.getRole().name() : null);
        dto.setVerified(user.isVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLocale(user.getLocale());
        return dto;
    }
}