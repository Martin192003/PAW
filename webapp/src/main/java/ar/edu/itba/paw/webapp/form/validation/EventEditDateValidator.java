package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.form.EventEditForm;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class EventEditDateValidator implements ConstraintValidator<ValidEventEditDate, EventEditForm> {

    @Override
    public boolean isValid(EventEditForm form, ConstraintValidatorContext context) {
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