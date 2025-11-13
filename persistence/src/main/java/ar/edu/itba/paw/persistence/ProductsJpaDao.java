package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.model.Club;
import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.model.ProductType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Repository
public class ProductsJpaDao implements ProductsDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Product create(Club club, String name, String description, BigDecimal basePrice,
                          ProductType type, boolean isAvailable) {

        Objects.requireNonNull(club, "club is required");
        Objects.requireNonNull(club.getClubId(), "clubId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(basePrice, "basePrice is required");
        if (basePrice.signum() < 0) throw new IllegalArgumentException("basePrice must be >= 0");
        Objects.requireNonNull(type, "type is required");

        Product p = new Product();
        p.setProductId(UUID.randomUUID());
        p.setClub(club);
        p.setProductName(name);
        p.setDescription(description);
        p.setBasePrice(basePrice);
        p.setProductType(type);
        p.setIsAvailable(isAvailable);
        // createdAt se setea en @PrePersist si viene nulo
        em.persist(p);
        return p;
    }

    @Override
    public Product update(UUID productId, UUID clubId, String name, String description,
                          BigDecimal basePrice, ProductType type, Boolean isAvailable) {

        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(clubId, "clubId is required");
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(basePrice, "basePrice is required");
        if (basePrice.signum() < 0) throw new IllegalArgumentException("basePrice must be >= 0");
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(isAvailable, "isAvailable is required");

        Product p = em.find(Product.class, productId);
        if (p == null) return null;

        Club clubRef = em.getReference(Club.class, clubId);

        p.setClub(clubRef);
        p.setProductName(name);
        p.setDescription(description);
        p.setBasePrice(basePrice);
        p.setProductType(type);
        p.setIsAvailable(isAvailable);

        return p; // entidad administrada ya quedó actualizada
    }

    @Override
    public boolean delete(UUID id) {
        Product p = em.find(Product.class, id);
        if (p == null) return false;
        em.remove(p);
        return true;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }


    @Override
    public List<Product> findByClub(final Club club) {
        Objects.requireNonNull(club, "club is required");
        Objects.requireNonNull(club.getClubId(), "clubId is required");

        // 1) IDs del club
        final String idSql = """
        SELECT CAST(p.product_id AS TEXT) AS id_text
        FROM products p
        WHERE p.club_id = :clubId
        ORDER BY p.product_name ASC NULLS LAST, p.product_id ASC
    """;

        @SuppressWarnings("unchecked")
        final List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("clubId", club.getClubId())
                .getResultList();

        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades
        return em.createQuery("""
            SELECT p
            FROM Product p
            WHERE p.productId IN :ids
            ORDER BY p.productName ASC, p.productId ASC
        """, Product.class)
                .setParameter("ids", ids)
                .getResultList();
    }


    @Override
    public List<Product> findByClubID(final UUID clubId) {
        Objects.requireNonNull(clubId, "clubId is required");

        // 1) IDs del club
        final String idSql = """
        SELECT CAST(p.product_id AS TEXT) AS id_text
        FROM products p
        WHERE p.club_id = :clubId
        ORDER BY p.product_name ASC NULLS LAST, p.product_id ASC
    """;

        @SuppressWarnings("unchecked")
        final List<String> idTexts = em.createNativeQuery(idSql)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("id_text", org.hibernate.type.StandardBasicTypes.STRING)
                .setParameter("clubId", clubId)
                .getResultList();

        if (idTexts.isEmpty()) return List.of();

        final List<UUID> ids = idTexts.stream().map(UUID::fromString).toList();

        // 2) Entidades
        return em.createQuery("""
            SELECT p
            FROM Product p
            WHERE p.productId IN :ids
            ORDER BY p.productName ASC, p.productId ASC
        """, Product.class)
                .setParameter("ids", ids)
                .getResultList();
    }

}
