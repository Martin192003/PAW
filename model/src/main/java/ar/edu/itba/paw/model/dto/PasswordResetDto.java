package ar.edu.itba.paw.model.dto;


public class PasswordResetDto {
    private String token;
    private String newPassword;
    private String confirmationPassword;

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @ApiFieldAccess(ApiFieldAccess.Mode.WRITE_ONLY)
    public String getConfirmationPassword() {
        return confirmationPassword;
    }

    public void setConfirmationPassword(String confirmationPassword) {
        this.confirmationPassword = confirmationPassword;
    }
}
