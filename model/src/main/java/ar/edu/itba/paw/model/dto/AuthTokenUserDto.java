package ar.edu.itba.paw.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuthTokenUserDto {
    private String href;
    private UUID userId;
    private String username;
    private String name;
    private String surname;
    private String email;
    private String cellphone;
    private String role;
    private boolean verified;
    private OffsetDateTime createdAt;
    private String locale;

    public AuthTokenUserDto() {
    }

    public AuthTokenUserDto(String href) {
        this.href = href;
    }

    public AuthTokenUserDto(String href,
                            UUID userId,
                            String username,
                            String name,
                            String surname,
                            String email,
                            String cellphone,
                            String role,
                            boolean verified,
                            OffsetDateTime createdAt,
                            String locale) {
        this.href = href;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.cellphone = cellphone;
        this.role = role;
        this.verified = verified;
        this.createdAt = createdAt;
        this.locale = locale;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

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

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
}
