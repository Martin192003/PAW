package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class CoverDto {
    private String clubId;
    private String image;

    public CoverDto() {}
    public CoverDto(String clubId, String image) {
        this.clubId = clubId;
        this.image = image;
    }
}
