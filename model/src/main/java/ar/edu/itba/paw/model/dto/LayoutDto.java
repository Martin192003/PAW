package ar.edu.itba.paw.model.dto;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class LayoutDto {

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private String clubId;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private String url;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private String image;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
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
