package ar.edu.itba.paw.model.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class UserDto {

    private UUID userId;
    private String username;
    private String name;
    private String surname;
    private String email;
    private String cellphone;
    private String role;
    private Boolean verified;
    private OffsetDateTime createdAt;
    private String locale;
    private List<LinkDto> links;

    public UserDto() {}

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCellphone() { return cellphone; }
    public void setCellphone(String cellphone) { this.cellphone = cellphone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean isVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public List<LinkDto> getLinks() { return links; }
    public void setLinks(List<LinkDto> links) { this.links = links; }
}
