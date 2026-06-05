package ar.edu.itba.paw.model.dto;

public class AuthTokenUserDto {
    private String href;

    public AuthTokenUserDto() {
    }

    public AuthTokenUserDto(String href) {
        this.href = href;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
