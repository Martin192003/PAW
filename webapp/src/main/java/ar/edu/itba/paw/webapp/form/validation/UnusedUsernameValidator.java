package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class UnusedUsernameValidator implements ConstraintValidator<UnusedUsername, String> {

    @Autowired
    private UserService userService;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.trim().isEmpty()) {
            return true; // lo valida @NotEmpty en el form
        }
        return userService.findByUserName(username) == null;
    }
}
