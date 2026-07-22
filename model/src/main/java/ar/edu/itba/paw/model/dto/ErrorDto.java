package ar.edu.itba.paw.model.dto;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ErrorDto {
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private String message;

    public ErrorDto() {}
    public ErrorDto(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
