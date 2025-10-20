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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ClubJpaDao implements ClubDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public Club create(String name, String description, String location, Price priceRange,
                       User contactInfo, MusicalGenre musicGenre, Zone zone, Boolean isBar) {

        final User contact = em.getReference(User.class, contactInfo.getId());

        final Club club = new Club(
                name,
                location,
                null,
                null,
                priceRange,
                description,
                contact,
                musicGenre,
                null,
                zone,
                isBar
        );

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


    @Override
    @Transactional
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
    @Transactional(readOnly = true)
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

    //FALTA TESTEAR
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

    //FALTA TESTEAR
    @Override
    public List<Club> findClubsWithBookingsByUserId(UUID userId) {
        return em.createQuery("""
            select distinct c
            from Club c
            join ClubTable t on t.clubId = c.clubId
            join Booking b on b.tableId = t.tableid
            where b.user.id = :uid
            order by c.createdAt desc
        """, Club.class)
        .setParameter("uid", userId)
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


    @Override
    public List<Club> searchPaginated(final String nameLike,
                                      final Zone location,
                                      final String musicGenre,
                                      final String priceRange,
                                      final Boolean isBar,
                                      final String sort,
                                      final int limit,
                                      final int offset) {
        // 1) IDs paginados (SQL nativo)
        final StringBuilder where = new StringBuilder("WHERE 1=1\n");
        if (nameLike != null && !nameLike.isBlank())       where.append("AND LOWER(c.club_name) LIKE :nameLike\n");
        if (location != null)                              where.append("AND c.zone = :zone\n");
        if (musicGenre != null && !musicGenre.isBlank())   where.append("AND UPPER(c.music_genre) = UPPER(:musicGenre)\n");
        if (priceRange != null && !priceRange.isBlank())   where.append("AND c.price_range = :priceRange\n");
        if (isBar != null)                                 where.append("AND c.is_bar = :isBar\n");

        final String inner = """
        SELECT DISTINCT c.club_id, c.created_at, c.club_name, c.rating
        FROM clubs c
        %s
    """.formatted(where);

        final String idSql = """
        SELECT CAST(x.club_id AS TEXT) AS id_text
        FROM ( %s ) x
        ORDER BY %s
    """.formatted(inner, nativeClubSort("x", sort));

        final var nq = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (nameLike != null && !nameLike.isBlank())     nq.setParameter("nameLike", "%" + nameLike.toLowerCase() + "%");
        if (location != null)                            nq.setParameter("zone", location.name()); // enum->texto
        if (musicGenre != null && !musicGenre.isBlank()) nq.setParameter("musicGenre", musicGenre.toUpperCase());
        if (priceRange != null && !priceRange.isBlank()) nq.setParameter("priceRange", Price.valueOf(priceRange).getLabel().toLowerCase()); // <- minúsculas DB
        if (isBar != null)                               nq.setParameter("isBar", isBar);

        @SuppressWarnings("unchecked")
        final List<String> idTexts = nq.getResultList();
        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades (JPQL)
        final String orderBy = jpqlClubSort(sort);
        return em.createQuery(
                        "SELECT c FROM Club c WHERE c.clubId IN :ids ORDER BY " + orderBy, Club.class)
                .setParameter("ids", ids)
                .getResultList();
    }


    @Override
    public int countSearchAvailable(String nameLike, Zone location, String musicGenre,
                                    String priceRange, Boolean isBar, java.time.LocalDate day) {
        final var zone = java.time.ZoneOffset.UTC; // cambiar según dominio
        final var dayStart = day.atStartOfDay().atOffset(zone);
        final var dayEnd   = day.plusDays(1).atStartOfDay().atOffset(zone);
        final var blocking = java.util.List.of(Status.CONFIRMADA, Status.PENDIENTE);

        StringBuilder jpql = new StringBuilder("""
        select count(distinct c)
        from Club c
        join ClubTable t on t.clubId = c.clubId
        where 1=1
    """);

        if (nameLike != null && !nameLike.isBlank()) jpql.append(" and lower(c.clubName) like :nameLike ");
        if (location != null) jpql.append(" and c.zone = :zone ");
        if (musicGenre != null && !musicGenre.isBlank()) jpql.append(" and c.musicGenre = :musicGenre ");
        if (priceRange != null && !priceRange.isBlank()) jpql.append(" and c.priceRange = :priceRange ");
        if (isBar != null) jpql.append(" and c.isBar = :isBar ");

        jpql.append("""
        and not exists (
            select 1 from Booking b
            where b.table = t
              and b.status in :blocking
              and b.bookingStart < :dayEnd
              and b.bookingEnd   > :dayStart
        )
    """);

        var q = em.createQuery(jpql.toString(), Long.class);

        if (nameLike != null && !nameLike.isBlank()) q.setParameter("nameLike", "%" + nameLike.toLowerCase() + "%");
        if (location != null) q.setParameter("zone", location);
        if (musicGenre != null && !musicGenre.isBlank()) q.setParameter("musicGenre", MusicalGenre.valueOf(musicGenre));
        if (priceRange != null && !priceRange.isBlank()) q.setParameter("priceRange", Price.valueOf(priceRange));
        if (isBar != null) q.setParameter("isBar", isBar);

        q.setParameter("blocking", blocking);
        q.setParameter("dayStart", dayStart);
        q.setParameter("dayEnd", dayEnd);

        return q.getSingleResult().intValue();
    }



    @Override
    public List<Club> searchAvailablePaginated(final String nameLike,
                                               final Zone location,
                                               final String musicGenre,
                                               final String priceRange,
                                               final Boolean isBar,
                                               final String sort,
                                               final java.time.LocalDate day,
                                               final int limit,
                                               final int offset) {
        final var zone = java.time.ZoneOffset.UTC;
        final var dayStart = day.atStartOfDay().atOffset(zone);
        final var dayEnd   = day.plusDays(1).atStartOfDay().atOffset(zone);
        final var blocking = java.util.List.of(Status.CONFIRMADA, Status.PENDIENTE);

        // 1) IDs paginados (SQL nativo con agregación de disponibilidad)
        final StringBuilder where = new StringBuilder("WHERE 1=1\n");
        if (nameLike != null && !nameLike.isBlank())       where.append("AND LOWER(c.club_name) LIKE :nameLike\n");
        if (location != null)                              where.append("AND c.zone = :zone\n");
        if (musicGenre != null && !musicGenre.isBlank())   where.append("AND UPPER(c.music_genre) = UPPER(:musicGenre)\n");
        if (priceRange != null && !priceRange.isBlank())   where.append("AND c.price_range = :priceRange\n");
        if (isBar != null)                                 where.append("AND c.is_bar = :isBar\n");

        final String inner = """
        SELECT
            c.club_id,
            c.created_at,
            COALESCE(SUM(CASE WHEN b.booking_id IS NULL THEN 1 ELSE 0 END), 0) AS availability
        FROM clubs c
        JOIN club_tables t ON t.club_id = c.club_id
        LEFT JOIN bookings b
               ON b.table_id = t.table_id
              AND LOWER(b.status) IN (:blocking)
              AND b.booking_start < :dayEnd
              AND b.booking_end   > :dayStart
        %s
        GROUP BY c.club_id, c.created_at
        HAVING COALESCE(SUM(CASE WHEN b.booking_id IS NULL THEN 1 ELSE 0 END), 0) > 0
    """.formatted(where);

        final String orderNative =
                ("availability_desc".equals(sort) || "availability_asc".equals(sort))
                        ? ("availability " + (sort != null && sort.endsWith("desc") ? "DESC" : "ASC") + ", x.created_at DESC, x.club_id DESC")
                        : nativeClubSort("x", sort);

        final String idSql = """
        SELECT CAST(x.club_id AS TEXT) AS id_text
        FROM ( %s ) x
        ORDER BY %s
    """.formatted(inner, orderNative);

        final var nq = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("blocking", blocking.stream().map(Status::db).map(String::toLowerCase).toList())
                .setParameter("dayStart", java.sql.Timestamp.from(dayStart.toInstant()))
                .setParameter("dayEnd",   java.sql.Timestamp.from(dayEnd.toInstant()))
                .setFirstResult(offset)
                .setMaxResults(limit);

        if (nameLike != null && !nameLike.isBlank())     nq.setParameter("nameLike", "%" + nameLike.toLowerCase() + "%");
        if (location != null)                            nq.setParameter("zone", location.name());
        if (musicGenre != null && !musicGenre.isBlank()) nq.setParameter("musicGenre", musicGenre.toUpperCase());
        if (priceRange != null && !priceRange.isBlank()) nq.setParameter("priceRange", Price.valueOf(priceRange).getLabel().toLowerCase()); // <- minúsculas DB
        if (isBar != null)                               nq.setParameter("isBar", isBar);

        @SuppressWarnings("unchecked")
        final List<String> idTexts = nq.getResultList();
        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades (JPQL)
        final String orderBy =
                ("availability_desc".equals(sort) || "availability_asc".equals(sort))
                        ? "/* orden principal ya viene de native */ c.createdAt DESC, c.clubId DESC"
                        : jpqlClubSort(sort);

        return em.createQuery(
                        "SELECT c FROM Club c WHERE c.clubId IN :ids ORDER BY " + orderBy, Club.class)
                .setParameter("ids", ids)
                .getResultList();
    }


    // Mapea el sort pedido a columnas nativas (con alias de subquery: "x")
    private static String nativeClubSort(final String a, final String sort) {
        final String alias = (a == null || a.isBlank()) ? "x" : a;
        final String key = (sort == null) ? "" : sort;
        return switch (key) {
            case "name_asc"  -> alias + ".club_name ASC, "  + alias + ".club_id ASC";
            case "name_desc" -> alias + ".club_name DESC, " + alias + ".club_id DESC";
            case "rating_asc"  -> alias + ".rating ASC NULLS LAST, "  + alias + ".club_id ASC";
            case "rating_desc" -> alias + ".rating DESC NULLS LAST, " + alias + ".club_id DESC";
            default /* created_desc */ ->
                    alias + ".created_at DESC NULLS LAST, " + alias + ".club_id DESC";
        };
    }

    // Mapea el sort a propiedades JPQL
    private static String jpqlClubSort(final String sort) {
        final String key = (sort == null) ? "" : sort;
        return switch (key) {
            case "name_asc"  -> "c.clubName ASC, c.clubId ASC";
            case "name_desc" -> "c.clubName DESC, c.clubId DESC";
            case "rating_asc"  -> "c.rating ASC, c.clubId ASC";
            case "rating_desc" -> "c.rating DESC, c.clubId DESC";
            default /* created_desc */ -> "c.createdAt DESC, c.clubId DESC";
        };
    }


    private static UUID parseUuidSafe(final String raw) {
        if (raw == null) throw new IllegalArgumentException("null uuid");
        String s = raw.trim();
        if (s.startsWith("{") && s.endsWith("}") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.length() > 36) {
            s = s.substring(0, 36);
        }
        return UUID.fromString(s);
    }

}