package ar.edu.itba.paw.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * DTO público: no expone email ni cellphone.
 * Se usa para responder GET /api/users/{userId} cuando el caller no es el propio usuario.
 */
public class PublicUserDto {

    private UUID userId;
    private String username;
    private String name;
    private String surname;
    private List<LinkDto> links;

    public PublicUserDto() {}

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public List<LinkDto> getLinks() { return links; }
    public void setLinks(List<LinkDto> links) { this.links = links; }
}
