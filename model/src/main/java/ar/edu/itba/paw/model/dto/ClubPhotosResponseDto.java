package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClubPhotosResponseDto {
    private List<ClubPhotoDto> photos = new ArrayList<>();

    public ClubPhotosResponseDto() {}

    public ClubPhotosResponseDto(List<ClubPhotoDto> photos) {
        this.photos = photos;
    }

    public List<ClubPhotoDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<ClubPhotoDto> photos) {
        this.photos = photos;
    }
}

