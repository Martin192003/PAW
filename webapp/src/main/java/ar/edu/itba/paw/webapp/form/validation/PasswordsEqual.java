package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsEqualValidator.class)
public @interface PasswordsEqual {
    String message() default "{register.password.mismatch}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
