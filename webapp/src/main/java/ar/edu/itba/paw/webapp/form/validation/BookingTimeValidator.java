package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.webapp.form.BookingForm;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BookingTimeValidator implements ConstraintValidator<ValidBookingTime, BookingForm> {

    @Override
    public boolean isValid(BookingForm form, ConstraintValidatorContext context) {
        if (form == null || form.getStart() == null || form.getEnd() == null) {
            return true;
        }
        return form.getEnd().isAfter(form.getStart());
    }
}
