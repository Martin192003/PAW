package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UnusedEmailValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnusedEmail {
    String message() default "{profile.email.duplicate}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
