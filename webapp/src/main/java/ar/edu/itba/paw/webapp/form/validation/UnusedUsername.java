package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UnusedUsernameValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnusedUsername {
    String message() default "{register.username.unused}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
