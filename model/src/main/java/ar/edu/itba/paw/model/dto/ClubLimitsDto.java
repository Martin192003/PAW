package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClubLimitsDto {

    private String clubId;
    private Integer maxCapacity;
    private Integer maxTables;
    private Integer minReservationPeople;
    private Integer maxReservationPeople;

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
}

