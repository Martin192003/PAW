package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LayoutDto {

    private String clubId;
    private String url;
    private String image;
    private List<LinkDto> links;

    public LayoutDto() {}

    public LayoutDto(String clubId, String image) {
        this.clubId = clubId;
        this.url = image;
        this.image = image;
    }

    public LayoutDto(String clubId, String image, List<LinkDto> links) {
        this(clubId, image);
        this.links = links;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
