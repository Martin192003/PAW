package ar.edu.itba.paw.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public class ClubAdminId implements java.io.Serializable {
    @Column(name = "club_id", columnDefinition = "UUID")
    private UUID clubId;

    @Column(name = "admin_id", columnDefinition = "UUID")
    private UUID adminId;

    public ClubAdminId() {}
    public ClubAdminId(UUID clubId, UUID adminId) {
        this.clubId = clubId;
        this.adminId = adminId;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClubAdminId)) return false;
        ClubAdminId that = (ClubAdminId) o;
        return clubId.equals(that.clubId) && adminId.equals(that.adminId);
    }

    @Override public int hashCode() {
        return 31 * clubId.hashCode() + adminId.hashCode();
    }

}