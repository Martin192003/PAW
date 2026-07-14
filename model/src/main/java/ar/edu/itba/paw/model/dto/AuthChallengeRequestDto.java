package ar.edu.itba.paw.model.dto;

public class AuthChallengeRequestDto {

    private String challengeType;
    private String email;
    private String usernameOrEmail;

    public AuthChallengeRequestDto() {}

    public String getChallengeType() { return challengeType; }
    public void setChallengeType(String challengeType) { this.challengeType = challengeType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
}
