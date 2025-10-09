package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
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
            SELECT COUNT(c)
            FROM Club c JOIN c.admins a
            WHERE c = :club AND a = :admin
        """, Long.class)
                .setParameter("club", club)
                .setParameter("admin", admin)
                .getSingleResult();
        return cnt > 0;
    }

    @Override
    public List<User> findAdmins(Club club) {
        return em.createQuery("""
            SELECT a
            FROM Club c JOIN c.admins a
            WHERE c = :club
            ORDER BY a.username ASC
        """, User.class)
                .setParameter("club", club)
                .getResultList();
    }

    @Override
    public List<Club> findClubsByAdmin(User admin) {
        return em.createQuery("""
            SELECT c
            FROM Club c JOIN c.admins a
            WHERE a = :admin
            ORDER BY c.clubName ASC
        """, Club.class)
                .setParameter("admin", admin)
                .getResultList();
    }

    @Override
    public boolean addAdmin(Club club, User admin) {
        Club managedClub = em.find(Club.class, club.getClubId());
        User adminRef = em.getReference(User.class, admin.getId());
        boolean added = managedClub.getAdmins().add(adminRef);
        return added;
    }

    @Override
    public boolean removeAdmin(Club club, User admin) {
        Club managedClub = em.find(Club.class, club.getClubId());
        User adminRef = em.getReference(User.class, admin.getId());
        boolean removed = managedClub.getAdmins().remove(adminRef);
        return removed;
    }
}
