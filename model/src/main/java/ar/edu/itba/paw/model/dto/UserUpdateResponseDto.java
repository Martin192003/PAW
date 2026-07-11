package ar.edu.itba.paw.model.dto;

public class UserUpdateResponseDto {

    private UserDto user;
    private String accessToken;
    private String tokenType;
    private long expiresIn;

    public UserUpdateResponseDto() {
    }

    public UserUpdateResponseDto(UserDto user, String accessToken, String tokenType, long expiresIn) {
        this.user = user;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
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
}
