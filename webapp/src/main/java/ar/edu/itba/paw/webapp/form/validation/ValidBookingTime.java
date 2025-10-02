package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingTimeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingTime {
    String message() default "{booking.time.invalid}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
