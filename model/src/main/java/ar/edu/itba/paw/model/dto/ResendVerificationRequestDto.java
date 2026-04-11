package ar.edu.itba.paw.model.dto;

public class ResendVerificationRequestDto {
    private String usernameOrEmail;

    public ResendVerificationRequestDto() {
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }
}

