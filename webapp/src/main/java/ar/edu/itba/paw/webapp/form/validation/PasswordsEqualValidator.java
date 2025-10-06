package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.form.UserForm;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordsEqualValidator implements ConstraintValidator<PasswordsEqual, UserForm> {

    @Override
    public boolean isValid(UserForm form, ConstraintValidatorContext context) {
        if (form == null) return true;
        String p1 = form.getPassword();
        String p2 = form.getPasswordRepeat();
        if (p1 == null || p2 == null) return true;
        return p1.equals(p2);
    }
}