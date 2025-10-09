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
    @Transactional
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
        p.setClub(club);                 // club debe ser entidad administrada o al menos tener ID válido
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
    @Transactional
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

        // Referencia liviana al Club (evita hit extra si no hace falta)
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
    @Transactional
    public boolean delete(UUID id) {
        Product p = em.find(Product.class, id);
        if (p == null) return false;
        em.remove(p);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findByName(String name) {
        return em.createQuery("""
                SELECT p
                FROM Product p
                WHERE LOWER(p.productName) = LOWER(:name)
                """, Product.class)
                .setParameter("name", name)
                .getResultList().stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return em.createQuery("""
                SELECT p
                FROM Product p
                ORDER BY p.productName ASC
                """, Product.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByClub(Club club) {
        return em.createQuery("""
                SELECT p
                FROM Product p
                WHERE p.club = :club
                ORDER BY p.productName ASC
                """, Product.class)
                .setParameter("club", club)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findByClubID(UUID clubId) {
        return em.createQuery("""
                SELECT p
                FROM Product p
                WHERE p.club.clubId = :cid
                ORDER BY p.productName ASC
                """, Product.class)
                .setParameter("cid", clubId)
                .getResultList();
    }
}
