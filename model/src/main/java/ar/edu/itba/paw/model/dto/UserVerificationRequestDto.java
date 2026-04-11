package ar.edu.itba.paw.model.dto;

public class UserVerificationRequestDto {
    private String token;
    private String code;

    public UserVerificationRequestDto() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}

