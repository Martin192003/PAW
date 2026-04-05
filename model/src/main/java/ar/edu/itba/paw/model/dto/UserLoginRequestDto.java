package ar.edu.itba.paw.model.dto;

public class UserLoginRequestDto {
    private String usernameOrEmail;
    private String password;

    public UserLoginRequestDto() {}

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}