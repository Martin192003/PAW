package ar.edu.itba.paw.model.dto;


public class EventPhotoDto {

    private String base64;

    private String filename;

    private String contentType; // "image/jpeg", "image/png", "image/webp"

    public String getBase64() { return base64; }
    public void setBase64(String base64) { this.base64 = base64; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
