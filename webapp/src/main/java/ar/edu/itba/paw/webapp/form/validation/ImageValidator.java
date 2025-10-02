package ar.edu.itba.paw.webapp.form.validation;

import org.springframework.web.multipart.MultipartFile;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        String contentType = file.getContentType();
        return contentType != null &&
                (contentType.equals("image/png") || contentType.equals("image/jpeg"));
    }
}