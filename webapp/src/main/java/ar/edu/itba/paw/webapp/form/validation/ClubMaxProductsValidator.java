package ar.edu.itba.paw.webapp.form.validation;

import ar.edu.itba.paw.model.exceptions.ClubNotFoundException;
import ar.edu.itba.paw.service.ClubService;
import ar.edu.itba.paw.webapp.form.AddProductForm;
import ar.edu.itba.paw.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.UUID;

@Component
public class ClubMaxProductsValidator implements ConstraintValidator<ClubMaxProducts, AddProductForm> {

    private int max;

    @Autowired
    private ProductService productService;

    @Autowired
    private ClubService clubService;

    @Override
    public void initialize(ClubMaxProducts constraintAnnotation) {
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(AddProductForm form, ConstraintValidatorContext context) {
        if (form == null) return true;
        UUID clubId = form.getClubId();
        if (clubId == null) return true;
        long count = productService.findByClub(clubService.getClubById(clubId).orElseThrow(ClubNotFoundException::new)).size();
        return count < max;
    }
}