package ar.edu.itba.paw.model.dto;


import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClubLimitsDto {

    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private String clubId;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private Integer maxCapacity;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private Integer maxTables;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private Integer minReservationPeople;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private Integer maxReservationPeople;
    @ApiFieldAccess(ApiFieldAccess.Mode.READ_ONLY)
    private List<LinkDto> links;

    public ClubLimitsDto() {}

    public ClubLimitsDto(String clubId,
                         Integer maxCapacity,
                         Integer maxTables,
                         Integer minReservationPeople,
                         Integer maxReservationPeople) {
        this.clubId = clubId;
        this.maxCapacity = maxCapacity;
        this.maxTables = maxTables;
        this.minReservationPeople = minReservationPeople;
        this.maxReservationPeople = maxReservationPeople;
    }

    public void setLinks(List<LinkDto> links) {
        this.links = links;
    }
}
