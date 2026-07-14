package ar.edu.itba.paw.model.dto;

public class AuthTokenRequestDto {

    private String grantType;
    private String email;
    private String usernameOrEmail;
    private String password;
    private String token;
    private String code;
    private String newPassword;
    private String confirmationPassword;

    public AuthTokenRequestDto() {}

    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmationPassword() { return confirmationPassword; }
    public void setConfirmationPassword(String confirmationPassword) { this.confirmationPassword = confirmationPassword; }
}
