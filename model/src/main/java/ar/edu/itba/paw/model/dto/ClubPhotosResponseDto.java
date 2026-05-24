package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClubPhotosResponseDto {
    private List<PhotoDto> photos = new ArrayList<>();

    public ClubPhotosResponseDto() {}

    public ClubPhotosResponseDto(List<PhotoDto> photos) {
        this.photos = photos;
    }

    public List<PhotoDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoDto> photos) {
        this.photos = photos;
    }
}

