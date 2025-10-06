package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.webapp.form.validation.ValidImage;
import org.springframework.web.multipart.MultipartFile;

public class ReceiptUploadForm {

    @ValidImage
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }
    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
