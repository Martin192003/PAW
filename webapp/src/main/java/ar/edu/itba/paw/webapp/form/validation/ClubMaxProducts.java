package ar.edu.itba.paw.webapp.form.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ClubMaxProductsValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ClubMaxProducts {
    String message() default "{max_products}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int max() default 50;
}