package ar.edu.itba.paw.model.dto;


public class AuthChallengeRequestDto {

    private String challengeType;
    private String email;
    private String usernameOrEmail;

    public AuthChallengeRequestDto() {}

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getChallengeType() { return challengeType; }
    public void setChallengeType(String challengeType) { this.challengeType = challengeType; }

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
}
