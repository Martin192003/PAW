package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.ClubLayout;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClubLayoutJpaDao implements ClubLayoutDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public ClubLayout save(ClubLayout layout) {
        if (layout.getLayoutId() == null) {
            em.persist(layout);
            return layout;
        } else {
            return em.merge(layout);
        }
    }

    @Override
    public Optional<ClubLayout> findByClubId(UUID clubId) {
        List<ClubLayout> result = em.createQuery(
                        "SELECT l FROM ClubLayout l WHERE l.clubId = :clubId", ClubLayout.class)
                .setParameter("clubId", clubId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public boolean deleteByClubId(UUID clubId) {
        int deleted = em.createQuery("DELETE FROM ClubLayout l WHERE l.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();
        em.flush();
        return deleted > 0;
    }
}

