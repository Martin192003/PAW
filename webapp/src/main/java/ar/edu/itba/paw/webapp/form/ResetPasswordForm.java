package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.PasswordMatches;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@PasswordMatches
public class ResetPasswordForm {

    @NotEmpty(message = "{register.password.notempty}")
    @Pattern(regexp = "^$|^.{6,}$", message = "{register.password.size}")
    private String newPassword;

    @NotEmpty(message = "{register.password.notempty}")
    private String confirmationPassword;

    public String getNewPassword() {
        return newPassword;
    }
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
    public String getConfirmationPassword() {
        return confirmationPassword;
    }
    public void setConfirmationPassword(String confirmationPassword) {
        this.confirmationPassword = confirmationPassword;
    }
}
