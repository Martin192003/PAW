package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EventEditDateValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEventEditDate {
    String message() default "{event.date.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}