package ar.edu.itba.paw.model.dto;

public class PasswordResetTokenDto {

    private String token;

    public PasswordResetTokenDto() {
    }

    public PasswordResetTokenDto(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
