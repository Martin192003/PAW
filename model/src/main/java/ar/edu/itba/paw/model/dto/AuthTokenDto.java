package ar.edu.itba.paw.model.dto;

public class AuthTokenDto {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private AuthTokenUserDto user;

    public AuthTokenDto() {
    }

    public AuthTokenDto(String accessToken, String tokenType, long expiresIn, AuthTokenUserDto user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public AuthTokenUserDto getUser() {
        return user;
    }

    public void setUser(AuthTokenUserDto user) {
        this.user = user;
    }
}
