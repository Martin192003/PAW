package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClubJpaDao implements ClubDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Club create(String name, String description, String location, Price priceRange, User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar) {
        final User managed = em.contains(contactInfo) ? contactInfo : em.find(User.class, contactInfo.getId());
        final Club club = new Club(UUID.randomUUID(), name, location, BigDecimal.ZERO, 0, priceRange,
                description, contactInfo, musicGenre, OffsetDateTime.now(), zone, isBar);
        club.getAdmins().add(managed);
        em.persist(club);
        return club;
    }

    @Override
    public Club update(UUID clubId, String name, String location, BigDecimal rating, Integer quantityRatings, Price priceRange, String description, UUID contactInfoId, MusicalGenre musicGenre, Zone zone, Boolean isBar) {
        final Club club = em.find(Club.class, clubId);
        if (club == null) {
            return null;
        }
        if (name != null) club.setClubName(name);
        if (location != null) club.setLocation(location);
        if (rating != null) club.setRating(rating);
        if (quantityRatings != null) club.setQuantityRatings(quantityRatings);
        if (priceRange != null) club.setPriceRange(priceRange);
        if (description != null) club.setDescription(description);
        if (contactInfoId != null) club.setContactInfo(em.find(User.class, contactInfoId));
        if (musicGenre != null) club.setMusicGenre(musicGenre);
        if (zone != null) club.setZone(zone);
        if (isBar != null) club.setIsBar(isBar);
        return club;
    }

    @Override
    public boolean delete(UUID clubId) {
        final Club club = em.find(Club.class, clubId);
        if (club == null) return false;
        em.remove(club);
        return true;
    }

    @Override
    public Optional<Club> findById(UUID clubId) {
        return Optional.ofNullable(em.find(Club.class, clubId));
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Club> findByName(String clubName) {
        final TypedQuery<Club> q = em.createQuery(
                "SELECT c FROM Club c WHERE c.clubName = :clubName", Club.class);
        q.setParameter("clubName", clubName);
        return q.getResultList().stream().findFirst();
    }

    @Override
    public List<Club> findAll() {
        return em.createQuery("SELECT c FROM Club c", Club.class)
                .getResultList();
    }

    @Override
    public Integer incrementQuantityRatings(UUID clubId, int delta) {
        final int updated = em.createQuery(
                        "UPDATE Club c SET c.quantityRatings = c.quantityRatings + :delta WHERE c.clubId = :id")
                .setParameter("delta", delta)
                .setParameter("id", clubId)
                .executeUpdate();
        if (updated == 0) return null;
        return em.createQuery(
                        "SELECT c.quantityRatings FROM Club c WHERE c.clubId = :id", Integer.class)
                .setParameter("id", clubId)
                .getSingleResult();
    }

    @Override
    public List<Club> search(String nameLike, Zone location, String musicGenre,
                             String priceRange, Boolean isBar, String sort) {
        final var cb = em.getCriteriaBuilder();
        final var cq = cb.createQuery(Club.class);
        final var root = cq.from(Club.class);

        final var predicates = new java.util.ArrayList<javax.persistence.criteria.Predicate>();

        if (nameLike != null && !nameLike.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("clubName")), "%" + nameLike.toLowerCase() + "%"));
        }
        if (location != null) {
            predicates.add(cb.equal(root.get("zone"), location));
        }
        if (musicGenre != null && !musicGenre.isBlank()) {
            predicates.add(cb.equal(root.get("musicGenre"), MusicalGenre.valueOf(musicGenre)));
        }
        if (priceRange != null && !priceRange.isBlank()) {
            predicates.add(cb.equal(root.get("priceRange"), Price.valueOf(priceRange)));
        }
        if (isBar != null) {
            predicates.add(cb.equal(root.get("isBar"), isBar));
        }

        cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[0]));

        String sortField = switch (sort == null ? "" : sort) {
            case "name_asc"  -> "clubName";
            case "name_desc" -> "clubName";
            case "rating_desc" -> "rating";
            case "rating_asc"  -> "rating";
            default -> null;
        };
        if (sortField != null) {
            boolean desc = sort.endsWith("desc");
            cq.orderBy(desc ? cb.desc(root.get(sortField)) : cb.asc(root.get(sortField)));
        }

        return em.createQuery(cq).getResultList();
    }


    @Override
    public List<Club> findByUserId(UUID userId) {
        return em.createQuery("""
        select distinct c
        from Club c
        left join c.admins a
        where a.id = :uid
           or c.contactInfo.id = :uid
        order by c.createdAt desc
        """, Club.class)
                .setParameter("uid", userId)
                .getResultList();
    }
    @Override
    public List<User> findAdminsByClubId(UUID clubId) {

        // 1) Admins del many-to-many (INNER JOIN => nunca trae nulls)
        List<User> admins = em.createQuery("""
        select distinct u
        from Club c
        join c.admins u
        where c.clubId = :cid
        """, User.class)
                .setParameter("cid", clubId)
                .getResultList();

        // 2) Contacto "legacy" (puede ser null)
        User contact = em.createQuery("""
        select c.contactInfo
        from Club c
        where c.clubId = :cid
        """, User.class)
                .setParameter("cid", clubId)
                .getSingleResult(); // devuelve 1 fila; el atributo puede ser null

        // 3) Merge sin NPEs ni duplicados (deduplico por UUID)
        java.util.LinkedHashMap<UUID, User> byId = new java.util.LinkedHashMap<>();
        if (contact != null && contact.getId() != null) {
            byId.put(contact.getId(), contact);
        }
        for (User u : admins) {
            if (u != null && u.getId() != null) {
                byId.putIfAbsent(u.getId(), u);
            }
        }
        return new java.util.ArrayList<>(byId.values());
    }



    @Override
    public List<Club> findAllPaginated(int limit, int offset) {
        return em.createQuery("SELECT c FROM Club c ORDER BY c.createdAt DESC", Club.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public int countAll() {
        return em.createQuery("SELECT COUNT(c) FROM Club c", Long.class)
                .getSingleResult()
                .intValue();
    }

    @Override
    public List<Club> searchPaginated(String nameLike, Zone location, String musicGenre,
                                      String priceRange, Boolean isBar, String sort,
                                      int limit, int offset) {
        final var cb = em.getCriteriaBuilder();
        final var cq = cb.createQuery(Club.class);
        final var root = cq.from(Club.class);

        final var predicates = buildSearchPredicates(cb, root, nameLike, location, musicGenre, priceRange, isBar);
        cq.where(predicates);

        applySort(cb, cq, root, sort);

        return em.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public int countSearch(String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar) {
        final var cb = em.getCriteriaBuilder();
        final var cq = cb.createQuery(Long.class);
        final var root = cq.from(Club.class);

        final var predicates = buildSearchPredicates(cb, root, nameLike, location, musicGenre, priceRange, isBar);
        cq.select(cb.count(root)).where(predicates);

        return em.createQuery(cq).getSingleResult().intValue();
    }

    @Override
    public List<Club> findByUserIdPaginated(UUID userId, int limit, int offset) {
        return em.createQuery("""
        select distinct c
        from Club c
        left join c.admins a
        where a.id = :uid
           or c.contactInfo.id = :uid
        order by c.createdAt desc
        """, Club.class)
                .setParameter("uid", userId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countByUserId(UUID userId) {
        Long n = em.createQuery("""
        select count(distinct c)
        from Club c
        left join c.admins a
        where a.id = :uid
           or c.contactInfo.id = :uid
        """, Long.class)
                .setParameter("uid", userId)
                .getSingleResult();
        return n.intValue();
    }


    @Override
    public List<UUID> findIdsByAdmin(UUID adminUserId) {
        return em.createQuery("""
        select distinct c.clubId
        from Club c
        left join c.admins a
        where a.id = :uid
           or c.contactInfo.id = :uid
        """, UUID.class)
                .setParameter("uid", adminUserId)
                .getResultList();
    }


    private Predicate[] buildSearchPredicates(
            CriteriaBuilder cb,
            Root<Club> root,
            String nameLike, Zone location, String musicGenre, String priceRange, Boolean isBar) {

        final var predicates = new java.util.ArrayList<javax.persistence.criteria.Predicate>();
        if (nameLike != null && !nameLike.isBlank())
            predicates.add(cb.like(cb.lower(root.get("clubName")), "%" + nameLike.toLowerCase() + "%"));
        if (location != null)
            predicates.add(cb.equal(root.get("zone"), location));
        if (musicGenre != null && !musicGenre.isBlank())
            predicates.add(cb.equal(root.get("musicGenre"), MusicalGenre.valueOf(musicGenre)));
        if (priceRange != null && !priceRange.isBlank())
            predicates.add(cb.equal(root.get("priceRange"), Price.valueOf(priceRange)));
        if (isBar != null)
            predicates.add(cb.equal(root.get("isBar"), isBar));

        return predicates.toArray(new javax.persistence.criteria.Predicate[0]);
    }

    private void applySort(javax.persistence.criteria.CriteriaBuilder cb,
                           javax.persistence.criteria.CriteriaQuery<Club> cq,
                           javax.persistence.criteria.Root<Club> root,
                           String sort) {
        String sortField = switch (sort == null ? "" : sort) {
            case "name_asc"  -> "clubName";
            case "name_desc" -> "clubName";
            case "rating_desc" -> "rating";
            case "rating_asc"  -> "rating";
            default -> null;
        };
        if (sortField != null) {
            boolean desc = sort.endsWith("desc");
            cq.orderBy(desc ? cb.desc(root.get(sortField)) : cb.asc(root.get(sortField)));
        }
    }
}