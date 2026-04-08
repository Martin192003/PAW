package ar.edu.itba.paw.model.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ClubPhotoDto {
    private String photoId;
    private String url;
    private String photoType;

    public ClubPhotoDto() {}

    public ClubPhotoDto(String photoId, String url, String photoType) {
        this.photoId = photoId;
        this.url = url;
        this.photoType = photoType;
    }

    public String getPhotoId() {
        return photoId;
    }

    public void setPhotoId(String photoId) {
        this.photoId = photoId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPhotoType() {
        return photoType;
    }

    public void setPhotoType(String photoType) {
        this.photoType = photoType;
    }
}


