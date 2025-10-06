package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.form.ResetPasswordForm;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, ResetPasswordForm> {

    @Override
    public boolean isValid(ResetPasswordForm form, ConstraintValidatorContext context) {
        if (form == null) return true;
        String p1 = form.getNewPassword();
        String p2 = form.getConfirmationPassword();
        if (p1 == null || p2 == null) return true;
        return p1.equals(p2);
    }
}

