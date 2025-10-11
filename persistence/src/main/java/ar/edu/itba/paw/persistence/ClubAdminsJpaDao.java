package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubAdmin;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class ClubAdminsJpaDao implements ClubAdminDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public boolean exists(Club club, User admin) {
        Long cnt = em.createQuery("""
            SELECT COUNT(ca) FROM ClubAdmin ca
            WHERE ca.id.clubId = :clubId AND ca.id.userId = :userId
        """, Long.class)
                .setParameter("clubId", club.getClubId())
                .setParameter("userId", admin.getId())
                .getSingleResult();
        return cnt > 0;
    }

    @Override
    public List<User> findAdmins(Club club) {
        return em.createQuery("""
            SELECT ca.admin FROM ClubAdmin ca
            WHERE ca.id.clubId = :clubId
            ORDER BY ca.admin.username ASC
        """, User.class)
                .setParameter("clubId", club.getClubId())
                .getResultList();
    }

    @Override
    public List<Club> findClubsByAdmin(User admin) {
        return em.createQuery("""
            SELECT ca.club FROM ClubAdmin ca
            WHERE ca.id.userId = :userId
            ORDER BY ca.club.clubName ASC
        """, Club.class)
                .setParameter("userId", admin.getId())
                .getResultList();
    }

    @Override
    public boolean addAdmin(Club club, User admin) {
        Club managedClub = em.find(Club.class, club.getClubId());
        if (managedClub == null) return false;
        em.persist(new ClubAdmin(managedClub, admin.getId()));
        return true;
    }

    @Override
    public boolean removeAdmin(Club club, User admin) {
        ClubAdmin.Id id = new ClubAdmin.Id(club.getClubId(), admin.getId());
        ClubAdmin link = em.find(ClubAdmin.class, id);
        if (link != null) {
            em.remove(link);
            return true;
        }
        return false;
    }
}
