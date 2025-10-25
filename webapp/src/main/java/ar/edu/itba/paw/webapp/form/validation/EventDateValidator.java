package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.form.EventAddForm;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class EventDateValidator implements ConstraintValidator<ValidEventDate, EventAddForm> {

    @Override
    public boolean isValid(EventAddForm form, ConstraintValidatorContext context) {
        if (form == null || form.getDate() == null) {
            return true;
        }
        boolean valid = !form.getDate().isBefore(LocalDate.now());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{event.date.invalid}")
                    .addPropertyNode("date")
                    .addConstraintViolation();
        }
        return valid;
    }
}
