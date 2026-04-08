package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LayoutDto {

    private String clubId;
    private String url;
    private String image;

    public LayoutDto() {}

    public LayoutDto(String clubId, String image) {
        this.clubId = clubId;
        this.url = image;
        this.image = image;
    }
}
