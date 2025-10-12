package ar.edu.itba.paw.model;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.User;

import javax.persistence.*;


@Entity
@Table(name = "club_admins")
public class ClubAdmin {

    @Embeddable
    public static class Id implements java.io.Serializable {
        @Column(name = "club_id", nullable = false)
        private java.util.UUID clubId;

        @Column(name = "admin_id", nullable = false)
        private java.util.UUID adminId;

        protected Id() {}
        public Id(java.util.UUID clubId, java.util.UUID adminId) {
            this.clubId = clubId;
            this.adminId = adminId;
        }
        @Override public boolean equals(Object o){
            if (this == o) return true;
            if (!(o instanceof Id)) return false;
            Id that = (Id) o;
            return java.util.Objects.equals(clubId, that.clubId)
                    && java.util.Objects.equals(adminId, that.adminId);
        }
        @Override public int hashCode(){ return java.util.Objects.hash(clubId, adminId); }
    }

    @EmbeddedId
    private Id id;

    @MapsId("clubId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    // Lectura del usuario por admin_id -> users.user_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", referencedColumnName = "user_id",
            insertable = false, updatable = false, nullable = false)
    private User admin;

    // Columna extra NOT NULL (no PK)
    @Column(name = "user_id", nullable = false)
    private java.util.UUID userId;

    protected ClubAdmin() {}

    public ClubAdmin(Club club, java.util.UUID userId) {
        this.club = club;
        this.id   = new Id(club.getClubId(), userId); // PK (club_id, admin_id)
        this.userId = userId;                          // tercera col NOT NULL
    }

    @PrePersist
    void prePersist() {
        if (userId == null && id != null) {
            userId = id.adminId; // garantizamos user_id = admin_id
        }
    }

    public Id getId() { return id; }
    public Club getClub() { return club; }
    public User getAdmin() { return admin; }
}
