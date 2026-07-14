package ar.edu.itba.paw.model.dto;

import java.util.UUID;

public class AuthTokenUserDto {
    private String href;
    private UUID userId;

    public AuthTokenUserDto() {
    }

    public AuthTokenUserDto(String href) {
        this.href = href;
    }

    public AuthTokenUserDto(String href, UUID userId) {
        this.href = href;
        this.userId = userId;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
